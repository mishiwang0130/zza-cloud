package com.wxy.common.security.defaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.common.redis.bo.TokenCacheBO;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.infra.api.client.InfraTokenClient;
import com.wxy.infra.api.dto.TokenCheckRespDTO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 默认令牌校验实现单元测试：缓存优先、未命中回源、Redis 异常降级、失败即拒绝。
 *
 * <p>这是一份被所有服务复用的默认实现，重点锁住「绝不能 fail-open」：
 * 缓存不可用要回源，回源失败要拒绝，不能因为依赖不可用就放行。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class DefaultTokenValidatorTest {

    /** Redis 读写工具 */
    @Mock
    private RedisUtil redisUtil;

    /** infra 凭证服务客户端 */
    @Mock
    private InfraTokenClient infraTokenClient;

    /** 被测实现 */
    private DefaultTokenValidator validator;

    /**
     * 装配被测实现
     */
    @BeforeEach
    void setUp() {
        validator = new DefaultTokenValidator(redisUtil, infraTokenClient);
    }

    /**
     * 缓存命中时直接返回身份，不再调 infra
     */
    @Test
    @DisplayName("validate：缓存命中时不回源")
    void shouldHitCache() {
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class)))
                .thenReturn(buildCache(1L, UserTypeEnum.ADMIN, LocalDateTime.now().plusHours(1)));

        LoginUser loginUser = validator.validate("token");

        assertThat(loginUser.userId()).isEqualTo(1L);
        assertThat(loginUser.userType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        verifyNoInteractions(infraTokenClient);
    }

    /**
     * 缓存未命中时回源调 infra
     */
    @Test
    @DisplayName("validate：缓存未命中时回源调 infra")
    void shouldFallbackToInfraWhenCacheMiss() {
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class))).thenReturn(null);
        when(infraTokenClient.checkToken(any()))
                .thenReturn(Result.success(new TokenCheckRespDTO(2L, UserTypeEnum.APP.getValue(), "app-user")));

        assertThat(validator.validate("token").userId()).isEqualTo(2L);
        verify(infraTokenClient).checkToken(any());
    }

    /**
     * 缓存已过期时回源（兜住「TTL 没设上」的异常写入）
     */
    @Test
    @DisplayName("validate：缓存已过期时回源调 infra")
    void shouldFallbackToInfraWhenCacheExpired() {
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class)))
                .thenReturn(buildCache(3L, UserTypeEnum.ADMIN, LocalDateTime.now().minusMinutes(1)));
        when(infraTokenClient.checkToken(any()))
                .thenReturn(Result.success(new TokenCheckRespDTO(3L, UserTypeEnum.ADMIN.getValue(), "admin")));

        assertThat(validator.validate("token").userId()).isEqualTo(3L);
        verify(infraTokenClient).checkToken(any());
    }

    /**
     * Redis 异常时降级为回源，不能因为缓存挂了就让鉴权不可用
     */
    @Test
    @DisplayName("validate：Redis 异常时降级为回源")
    void shouldFallbackToInfraWhenRedisFails() {
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class))).thenThrow(new RuntimeException("redis down"));
        when(infraTokenClient.checkToken(any()))
                .thenReturn(Result.success(new TokenCheckRespDTO(4L, UserTypeEnum.ADMIN.getValue(), "admin")));

        assertThat(validator.validate("token").userId()).isEqualTo(4L);
    }

    /**
     * 回源拿到「未登录」时原样抛 401，前端据此跳登录
     */
    @Test
    @DisplayName("validate：回源返回未登录时抛 401")
    void shouldThrowUnauthorizedWhenRemoteRejects() {
        when(redisUtil.get(anyString(), eq(TokenCacheBO.class))).thenReturn(null);
        when(infraTokenClient.checkToken(any())).thenReturn(Result.error(CommonErrorConstant.UNAUTHORIZED));

        assertThatThrownBy(() -> validator.validate("token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 构造缓存内容
     *
     * @param userId     用户 ID
     * @param userType   端类型
     * @param expireTime 过期时间
     * @return 缓存内容
     */
    private TokenCacheBO buildCache(Long userId, UserTypeEnum userType, LocalDateTime expireTime) {
        TokenCacheBO cache = new TokenCacheBO();
        cache.setUserId(userId);
        cache.setUserType(userType.getValue());
        cache.setUsername("admin");
        cache.setExpireTime(expireTime);
        return cache;
    }
}
