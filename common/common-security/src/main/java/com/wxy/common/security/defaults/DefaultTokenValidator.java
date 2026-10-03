package com.wxy.common.security.defaults;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.common.core.util.DigestUtil;
import com.wxy.common.redis.bo.TokenCacheBO;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.common.redis.util.TokenCacheKeyUtil;
import com.wxy.infra.api.client.InfraTokenClient;
import com.wxy.infra.api.dto.TokenCheckReqDTO;
import com.wxy.infra.api.dto.TokenCheckRespDTO;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认的令牌校验实现：先读平台凭证缓存，未命中或失效再回源调 infra。
 *
 * <p>大多数服务没有自己的用户与凭证数据，鉴权就是「问 infra + 读共享缓存」，所以 common 直接给一份
 * 默认实现，服务引了 common-security 就能用；真的有自己的用户表、凭证表时，自己定义同类型的
 * {@code TokenValidator} Bean 覆盖即可（自动装配带 {@code @ConditionalOnMissingBean}）。
 *
 * <p>两条路径的取舍：
 * <ul>
 *   <li><b>缓存命中</b>：直接用缓存里的身份，不需要验签、不需要调 infra——缓存里有这个令牌的记录，
 *       本身就说明签发方校验过它，顺带也省掉了给每个服务分发 JWT 密钥；</li>
 *   <li><b>缓存未命中/已过期/内容不完整/Redis 不可用</b>：一律回源调 infra，
 *       由签发方给出权威结论（登出、续期轮换、改密这些状态只有它知道）。</li>
 * </ul>
 *
 * <p>远程调用带 Sentinel 熔断降级，且降级一律**拒绝**：鉴权不能因为依赖不可用就放行。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
public class DefaultTokenValidator implements TokenValidator {

    /** Sentinel 资源名：配置熔断/限流规则时用它定位 */
    public static final String SENTINEL_RESOURCE = "defaultTokenValidator:validate";

    /** Redis 读写工具：读平台凭证缓存 */
    private final RedisUtil redisUtil;

    /** infra 的凭证服务：缓存未命中时回源校验 */
    private final InfraTokenClient infraTokenClient;

    /**
     * 构造校验器
     *
     * @param redisUtil      Redis 读写工具
     * @param infraTokenClient infra 凭证服务客户端
     */
    public DefaultTokenValidator(RedisUtil redisUtil, InfraTokenClient infraTokenClient) {
        this.redisUtil = redisUtil;
        this.infraTokenClient = infraTokenClient;
    }

    /**
     * 校验令牌：缓存优先，未命中回源
     *
     * @param token 裸令牌（已去掉 Bearer 前缀）
     * @return 登录用户
     */
    @Override
    @SentinelResource(value = SENTINEL_RESOURCE,
            blockHandler = "validateBlocked",
            fallback = "validateFailed")
    public LoginUser validate(String token) {
        LoginUser cached = readCache(token);
        return cached != null ? cached : validateRemote(token);
    }

    /**
     * 读平台凭证缓存
     *
     * <p>Redis 异常时不往外抛：缓存只是省一次远程调用，挂了也应该降级为回源，
     * 而不是让整个鉴权链路不可用（正确性由回源保证）。
     *
     * @param token 裸令牌
     * @return 缓存中的登录用户，缓存不可用或内容不可用时返回 null
     */
    private LoginUser readCache(String token) {
        try {
            TokenCacheBO cache = redisUtil.get(
                    TokenCacheKeyUtil.accessTokenKey(DigestUtil.sha256Hex(token)), TokenCacheBO.class);
            if (cache == null || cache.getUserId() == null) {
                return null;
            }
            if (cache.getExpireTime() != null && cache.getExpireTime().isBefore(LocalDateTime.now())) {
                // 正常过期由 Redis TTL 清理，这里兜住「TTL 没设上」的异常写入
                return null;
            }
            return new LoginUser(cache.getUserId(), cache.getUserType(), cache.getUsername());
        } catch (RuntimeException ex) {
            log.warn("[readCache][读取平台凭证缓存失败，降级为回源校验] error={}", ex.getMessage());
            return null;
        }
    }

    /**
     * 回源校验：调 infra 的凭证服务
     *
     * @param token 裸令牌
     * @return 登录用户
     */
    private LoginUser validateRemote(String token) {
        TokenCheckReqDTO reqDTO = new TokenCheckReqDTO();
        reqDTO.setToken(token);
        TokenCheckRespDTO dto = infraTokenClient.checkToken(reqDTO).requireData();
        return new LoginUser(dto.getUserId(), dto.getUserType(), dto.getUsername());
    }

    /**
     * 熔断/限流降级：拒绝访问
     *
     * @param token 裸令牌
     * @param ex    Sentinel 阻塞异常
     * @return 不会正常返回
     */
    public LoginUser validateBlocked(String token, BlockException ex) {
        log.error("[validateBlocked][令牌校验被熔断或限流，按未登录拒绝] rule={}", ex.getRule());
        throw new BizException(CommonErrorConstant.REMOTE_CALL_ERROR, "鉴权服务繁忙，请稍后重试");
    }

    /**
     * 异常降级：拒绝访问
     *
     * <p>令牌无效本来就是 {@code UnauthorizedException}（401），属于正常业务结果，原样抛给调用方，
     * 前端据此跳登录；其他异常（远程调用失败等）统一转成「服务调用失败」，避免暴露内部细节。
     *
     * @param token 裸令牌
     * @param ex    业务异常或调用异常
     * @return 不会正常返回
     */
    public LoginUser validateFailed(String token, Throwable ex) {
        if (ex instanceof BizException bizException) {
            throw bizException;
        }
        log.error("[validateFailed][令牌校验失败，按未登录拒绝]", ex);
        throw new BizException(CommonErrorConstant.REMOTE_CALL_ERROR, "鉴权服务异常，请稍后重试");
    }
}
