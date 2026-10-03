package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.common.security.util.JwtUtil;
import com.wxy.infra.biz.bo.InfraTokenCacheBO;
import com.wxy.infra.biz.config.InfraTokenProperties;
import com.wxy.infra.biz.constant.InfraConstant;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.mapper.InfraTokenMapper;
import com.wxy.infra.biz.mapper.InfraTokenRefreshMapper;
import com.wxy.infra.biz.po.InfraToken;
import com.wxy.infra.biz.po.InfraTokenRefresh;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.util.InfraRedisKeyUtil;
import com.wxy.infra.biz.util.InfraTokenUtil;
import com.wxy.infra.biz.vo.admin.AuthTokenRespVO;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 凭证服务实现：access token 用 JWT，续期凭证用不透明随机串，两者都只把摘要落库。
 *
 * <p>校验策略是「Redis 优先、MySQL 权威」：命中缓存直接返回，未命中回查 MySQL 并回写缓存；
 * 登出与续期轮换会同时改库并删缓存，所以缓存不会长期保留已失效凭证。
 *
 * <p>端无关：admin 端与 app 端共用一个实现，凭证上的 {@code userType} 决定它属于哪一端。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Service
public class InfraTokenServiceImpl implements InfraTokenService {

    /** JWT 工具：签发与解析 access token */
    @Resource
    private JwtUtil jwtUtil;

    /** Redis 读写工具 */
    @Resource
    private RedisUtil redisUtil;

    /** 访问凭证 Mapper */
    @Resource
    private InfraTokenMapper infraTokenMapper;

    /** 续期凭证 Mapper */
    @Resource
    private InfraTokenRefreshMapper infraTokenRefreshMapper;

    /** 凭证配置：续期凭证有效期 */
    @Resource
    private InfraTokenProperties infraTokenProperties;

    /**
     * 签发一对凭证并落库
     *
     * @param userId   用户 ID
     * @param userType 登录端类型
     * @param username 登录用户名
     * @param loginIp  登录 IP，可为空
     * @return 凭证返回体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenRespVO createTokenPair(Long userId, UserTypeEnum userType, String username, String loginIp) {
        LocalDateTime now = LocalDateTime.now();
        String accessToken = jwtUtil.generate(new LoginUser(userId, userType.getValue(), username));
        String refreshToken = InfraTokenUtil.generateRefreshToken();
        String accessHash = InfraTokenUtil.sha256Hex(accessToken);
        String refreshHash = InfraTokenUtil.sha256Hex(refreshToken);
        LocalDateTime accessExpireTime = now.plusSeconds(jwtUtil.getExpireSeconds());
        LocalDateTime refreshExpireTime = now.plusSeconds(infraTokenProperties.getRefreshExpireSeconds());

        InfraToken accessRecord = new InfraToken();
        accessRecord.setTokenHash(accessHash);
        accessRecord.setUserId(userId);
        accessRecord.setUserType(userType.getValue());
        accessRecord.setUsername(username);
        accessRecord.setExpireTime(accessExpireTime);
        accessRecord.setStatus(CommonStatusEnum.ENABLED.getValue());
        accessRecord.setLoginIp(loginIp);
        accessRecord.setRefreshTokenHash(refreshHash);
        infraTokenMapper.insert(accessRecord);

        InfraTokenRefresh refreshRecord = new InfraTokenRefresh();
        refreshRecord.setTokenHash(refreshHash);
        refreshRecord.setUserId(userId);
        refreshRecord.setUserType(userType.getValue());
        refreshRecord.setUsername(username);
        refreshRecord.setExpireTime(refreshExpireTime);
        refreshRecord.setStatus(CommonStatusEnum.ENABLED.getValue());
        refreshRecord.setLoginIp(loginIp);
        infraTokenRefreshMapper.insert(refreshRecord);

        cacheAccessToken(accessHash, buildCache(userId, userType.getValue(), username, accessExpireTime, loginIp));
        cacheRefreshToken(refreshHash, buildCache(userId, userType.getValue(), username, refreshExpireTime, loginIp));
        return new AuthTokenRespVO(accessToken, InfraConstant.TOKEN_TYPE_BEARER, jwtUtil.getExpireSeconds(), refreshToken);
    }

    /**
     * 校验访问凭证
     *
     * @param authorization  请求头原值
     * @param expectUserType 期望的登录端类型
     * @return 登录用户
     */
    @Override
    public LoginUser validate(String authorization, UserTypeEnum expectUserType) {
        String accessToken = InfraTokenUtil.stripBearer(authorization);
        if (!StringUtils.hasText(accessToken)) {
            throw new UnauthorizedException();
        }
        // 先验签并校验 JWT 自身的过期时间：伪造的 token 不必去查缓存与数据库
        jwtUtil.parse(accessToken);
        String tokenHash = InfraTokenUtil.sha256Hex(accessToken);
        InfraTokenCacheBO cache = redisUtil.get(InfraRedisKeyUtil.accessTokenKey(tokenHash), InfraTokenCacheBO.class);
        if (cache == null) {
            InfraToken record = infraTokenMapper.selectOne(new LambdaQueryWrapper<InfraToken>()
                    .eq(InfraToken::getTokenHash, tokenHash)
                    .eq(InfraToken::getStatus, CommonStatusEnum.ENABLED.getValue())
                    .gt(InfraToken::getExpireTime, LocalDateTime.now()));
            if (record == null) {
                throw new UnauthorizedException();
            }
            cache = buildCache(record.getUserId(), record.getUserType(), record.getUsername(),
                    record.getExpireTime(), record.getLoginIp());
            cacheAccessToken(tokenHash, cache);
        }
        assertUserType(cache.getUserType(), expectUserType);
        return new LoginUser(cache.getUserId(), cache.getUserType(), cache.getUsername());
    }

    /**
     * 用续期凭证换取新的一对凭证
     *
     * @param refreshToken   续期凭证原值
     * @param expectUserType 期望的登录端类型
     * @return 新的凭证返回体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenRespVO refresh(String refreshToken, UserTypeEnum expectUserType) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(InfraErrorConstant.REFRESH_TOKEN_INVALID);
        }
        String refreshHash = InfraTokenUtil.sha256Hex(refreshToken);
        InfraTokenCacheBO cache = loadRefreshCache(refreshHash);
        if (cache == null) {
            throw new BizException(InfraErrorConstant.REFRESH_TOKEN_INVALID);
        }
        if (expectUserType != null && !expectUserType.getValue().equals(cache.getUserType())) {
            throw new BizException(InfraErrorConstant.TOKEN_USER_TYPE_MISMATCH);
        }
        UserTypeEnum userType = UserTypeEnum.of(cache.getUserType());
        if (userType == null) {
            throw new BizException(InfraErrorConstant.TOKEN_USER_TYPE_MISMATCH);
        }
        // 轮换：旧续期凭证立即失效，同一个凭证不能用第二次
        disableRefreshToken(refreshHash);
        return createTokenPair(cache.getUserId(), userType, cache.getUsername(), cache.getLoginIp());
    }

    /**
     * 失效当前凭证
     *
     * @param authorization 请求头原值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String authorization) {
        String accessToken = InfraTokenUtil.stripBearer(authorization);
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        String tokenHash = InfraTokenUtil.sha256Hex(accessToken);
        InfraToken record = infraTokenMapper.selectOne(new LambdaQueryWrapper<InfraToken>()
                .eq(InfraToken::getTokenHash, tokenHash));
        if (record == null) {
            // 幂等：凭证不存在（例如已清理）时直接返回，登出接口重复调用不应报错
            return;
        }
        record.setStatus(CommonStatusEnum.DISABLED.getValue());
        infraTokenMapper.updateById(record);
        redisUtil.delete(InfraRedisKeyUtil.accessTokenKey(tokenHash));
        if (StringUtils.hasText(record.getRefreshTokenHash())) {
            disableRefreshToken(record.getRefreshTokenHash());
        }
    }

    /**
     * 失效某个用户的全部有效凭证
     *
     * @param userId 用户 ID，为 null 时忽略
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAll(Long userId) {
        if (userId == null) {
            return;
        }
        List<InfraToken> tokens = infraTokenMapper.selectList(new LambdaQueryWrapper<InfraToken>()
                .eq(InfraToken::getUserId, userId)
                .eq(InfraToken::getStatus, CommonStatusEnum.ENABLED.getValue()));
        for (InfraToken token : tokens) {
            token.setStatus(CommonStatusEnum.DISABLED.getValue());
            infraTokenMapper.updateById(token);
            redisUtil.delete(InfraRedisKeyUtil.accessTokenKey(token.getTokenHash()));
            if (StringUtils.hasText(token.getRefreshTokenHash())) {
                disableRefreshToken(token.getRefreshTokenHash());
            }
        }
    }

    /**
     * 读取续期凭证缓存，未命中回查数据库并回写
     *
     * @param refreshHash 续期凭证摘要
     * @return 凭证缓存对象，无效时返回 null
     */
    private InfraTokenCacheBO loadRefreshCache(String refreshHash) {
        String key = InfraRedisKeyUtil.refreshTokenKey(refreshHash);
        InfraTokenCacheBO cache = redisUtil.get(key, InfraTokenCacheBO.class);
        if (cache != null) {
            if (cache.getExpireTime() == null || cache.getExpireTime().isBefore(LocalDateTime.now())) {
                // 续期凭证不是 JWT，过期只能自己判断；过期后顺手清掉缓存，避免每次都读到失效数据
                redisUtil.delete(key);
                return null;
            }
            return cache;
        }
        InfraTokenRefresh record = infraTokenRefreshMapper.selectOne(new LambdaQueryWrapper<InfraTokenRefresh>()
                .eq(InfraTokenRefresh::getTokenHash, refreshHash)
                .eq(InfraTokenRefresh::getStatus, CommonStatusEnum.ENABLED.getValue())
                .gt(InfraTokenRefresh::getExpireTime, LocalDateTime.now()));
        if (record == null) {
            return null;
        }
        cache = buildCache(record.getUserId(), record.getUserType(), record.getUsername(),
                record.getExpireTime(), record.getLoginIp());
        cacheRefreshToken(refreshHash, cache);
        return cache;
    }

    /**
     * 把续期凭证置为失效并删除缓存
     *
     * @param refreshHash 续期凭证摘要
     */
    private void disableRefreshToken(String refreshHash) {
        InfraTokenRefresh refresh = infraTokenRefreshMapper.selectOne(new LambdaQueryWrapper<InfraTokenRefresh>()
                .eq(InfraTokenRefresh::getTokenHash, refreshHash));
        if (refresh != null && !CommonStatusEnum.DISABLED.getValue().equals(refresh.getStatus())) {
            refresh.setStatus(CommonStatusEnum.DISABLED.getValue());
            infraTokenRefreshMapper.updateById(refresh);
        }
        redisUtil.delete(InfraRedisKeyUtil.refreshTokenKey(refreshHash));
    }

    /**
     * 校验凭证端类型与请求所在端一致
     *
     * @param userType       凭证记录的端类型
     * @param expectUserType 期望的端类型
     */
    private void assertUserType(Integer userType, UserTypeEnum expectUserType) {
        if (expectUserType == null || expectUserType.getValue().equals(userType)) {
            return;
        }
        throw new UnauthorizedException(InfraErrorConstant.TOKEN_USER_TYPE_MISMATCH.msg());
    }

    /**
     * 构造缓存对象
     *
     * @param userId     用户 ID
     * @param userType   端类型
     * @param username   用户名
     * @param expireTime 过期时间
     * @param loginIp    登录 IP
     * @return 缓存对象
     */
    private InfraTokenCacheBO buildCache(Long userId, Integer userType, String username,
                                         LocalDateTime expireTime, String loginIp) {
        InfraTokenCacheBO cache = new InfraTokenCacheBO();
        cache.setUserId(userId);
        cache.setUserType(userType);
        cache.setUsername(username);
        cache.setExpireTime(expireTime);
        cache.setLoginIp(loginIp);
        return cache;
    }

    /**
     * 写访问凭证缓存，TTL 取剩余有效期
     *
     * @param tokenHash 凭证摘要
     * @param cache     缓存对象
     */
    private void cacheAccessToken(String tokenHash, InfraTokenCacheBO cache) {
        long ttl = remainingSeconds(cache.getExpireTime());
        if (ttl <= 0) {
            return;
        }
        redisUtil.set(InfraRedisKeyUtil.accessTokenKey(tokenHash), cache, ttl, TimeUnit.SECONDS);
    }

    /**
     * 写续期凭证缓存，TTL 取剩余有效期
     *
     * @param refreshHash 续期凭证摘要
     * @param cache       缓存对象
     */
    private void cacheRefreshToken(String refreshHash, InfraTokenCacheBO cache) {
        long ttl = remainingSeconds(cache.getExpireTime());
        if (ttl <= 0) {
            return;
        }
        redisUtil.set(InfraRedisKeyUtil.refreshTokenKey(refreshHash), cache, ttl, TimeUnit.SECONDS);
    }

    /**
     * 计算距离过期还剩多少秒
     *
     * @param expireTime 过期时间，可以为 null
     * @return 剩余秒数，已过期或入参为 null 时返回 0
     */
    private long remainingSeconds(LocalDateTime expireTime) {
        if (expireTime == null) {
            return 0L;
        }
        long seconds = Duration.between(LocalDateTime.now(), expireTime).getSeconds();
        return Math.max(seconds, 0L);
    }
}
