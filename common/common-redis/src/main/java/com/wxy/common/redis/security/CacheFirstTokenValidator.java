package com.wxy.common.redis.security;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.common.core.util.DigestUtil;
import com.wxy.common.redis.bo.TokenCacheBO;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.common.redis.util.TokenCacheKeyUtil;
import java.time.LocalDateTime;

/**
 * 「缓存优先、兜底回源」的令牌校验实现：先读平台凭证缓存，未命中或已过期再交给兜底实现。
 *
 * <p>为什么这么拆：凭证状态（是否登出、是否被轮换）由签发凭证的服务说了算，必须回源；
 * 但每次都走一次跨服务调用没必要——签发方已经把校验结果写进 Redis，其他服务直接读同一份缓存即可。
 * 缓存未命中时再调签发方（兜底实现通常是「调 infra 的校验接口」），本次调用也会顺带把缓存补齐。
 *
 * <p>安全上要注意：这条路径的前提是缓存的 key 与结构两边一致（由 common-redis 统一维护），
 * 且 Redis 只在内网可达；缓存里没有记录时一律回源，不做「缓存没查到就放行」的兜底。
 *
 * @author wxy
 * @date 2026/10/03
 */
public class CacheFirstTokenValidator implements TokenValidator {

    /** Redis 读写工具 */
    private final RedisUtil redisUtil;

    /** 兜底实现：缓存未命中时调签发凭证的服务校验 */
    private final TokenValidator fallback;

    /**
     * 构造校验器
     *
     * @param redisUtil Redis 读写工具
     * @param fallback  兜底校验实现，通常是「调 infra 校验接口」的实现
     */
    public CacheFirstTokenValidator(RedisUtil redisUtil, TokenValidator fallback) {
        this.redisUtil = redisUtil;
        this.fallback = fallback;
    }

    /**
     * 校验令牌：缓存优先，未命中或已过期回源
     *
     * @param token 裸令牌（已去掉 Bearer 前缀）
     * @return 登录用户
     */
    @Override
    public LoginUser validate(String token) {
        TokenCacheBO cache = redisUtil.get(
                TokenCacheKeyUtil.accessTokenKey(DigestUtil.sha256Hex(token)), TokenCacheBO.class);
        if (isValid(cache)) {
            return new LoginUser(cache.getUserId(), cache.getUserType(), cache.getUsername());
        }
        return fallback.validate(token);
    }

    /**
     * 判断缓存内容是否可用
     *
     * <p>凭证本身可带 TTL，正常情况下过期即被 Redis 清理；这里再校验一次过期时间，
     * 兜住「TTL 没设上」这类异常写入，避免过期凭证被当成有效。
     *
     * @param cache 缓存内容，可以为 null
     * @return 可用时返回 true
     */
    private boolean isValid(TokenCacheBO cache) {
        return cache != null
                && cache.getUserId() != null
                && (cache.getExpireTime() == null || cache.getExpireTime().isAfter(LocalDateTime.now()));
    }
}
