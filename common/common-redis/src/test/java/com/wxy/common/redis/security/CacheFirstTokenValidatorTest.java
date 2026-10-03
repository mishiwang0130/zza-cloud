package com.wxy.common.redis.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.common.redis.bo.TokenCacheBO;
import com.wxy.common.redis.util.RedisUtil;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 「缓存优先、兜底回源」校验器单元测试。
 *
 * <p>重点：缓存只用来省一次跨服务调用，绝不能变成「缓存没查到就放行」——
 * 未命中、已过期、内容不完整这三类情况都必须回源。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class CacheFirstTokenValidatorTest {

    /** Redis 读写工具 */
    @Mock
    private RedisUtil redisUtil;

    /** 兜底实现：这里模拟「调 infra 校验」 */
    @Mock
    private TokenValidator fallback;

    /** 被测校验器 */
    private CacheFirstTokenValidator validator;

    /**
     * 装配被测校验器
     */
    @BeforeEach
    void setUp() {
        validator = new CacheFirstTokenValidator(redisUtil, fallback);
    }

    /**
     * 缓存命中时直接返回身份，不再回源
     */
    @Test
    @DisplayName("validate：缓存命中时不回源")
    void shouldHitCache() {
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class)))
                .thenReturn(buildCache(1L, LocalDateTime.now().plusHours(1)));

        LoginUser loginUser = validator.validate("token");

        assertThat(loginUser.userId()).isEqualTo(1L);
        assertThat(loginUser.userType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        verifyNoInteractions(fallback);
    }

    /**
     * 缓存未命中时回源，由签发凭证的服务给出结论
     */
    @Test
    @DisplayName("validate：缓存未命中时回源")
    void shouldFallbackWhenCacheMiss() {
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class))).thenReturn(null);
        when(fallback.validate("token"))
                .thenReturn(new LoginUser(2L, UserTypeEnum.APP.getValue(), "app-user"));

        assertThat(validator.validate("token").userId()).isEqualTo(2L);
        verify(fallback).validate("token");
    }

    /**
     * 缓存已过期时回源，避免「TTL 没设上」导致过期凭证被当成有效
     */
    @Test
    @DisplayName("validate：缓存已过期时回源")
    void shouldFallbackWhenCacheExpired() {
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class)))
                .thenReturn(buildCache(3L, LocalDateTime.now().minusMinutes(1)));
        when(fallback.validate("token"))
                .thenReturn(new LoginUser(3L, UserTypeEnum.ADMIN.getValue(), "admin"));

        assertThat(validator.validate("token").userId()).isEqualTo(3L);
        verify(fallback).validate("token");
    }

    /**
     * 缓存内容不完整（没有用户 ID）时同样回源，不拿脏数据当身份
     */
    @Test
    @DisplayName("validate：缓存内容不完整时回源")
    void shouldFallbackWhenCacheIncomplete() {
        TokenCacheBO cache = buildCache(null, LocalDateTime.now().plusHours(1));
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class))).thenReturn(cache);
        when(fallback.validate("token"))
                .thenReturn(new LoginUser(4L, UserTypeEnum.ADMIN.getValue(), "admin"));

        assertThat(validator.validate("token").userId()).isEqualTo(4L);
        verify(fallback).validate("token");
    }

    /**
     * 构造缓存内容
     *
     * @param userId     用户 ID
     * @param expireTime 过期时间
     * @return 缓存内容
     */
    private TokenCacheBO buildCache(Long userId, LocalDateTime expireTime) {
        TokenCacheBO cache = new TokenCacheBO();
        cache.setUserId(userId);
        cache.setUserType(UserTypeEnum.ADMIN.getValue());
        cache.setUsername("admin");
        cache.setExpireTime(expireTime);
        return cache;
    }
}
