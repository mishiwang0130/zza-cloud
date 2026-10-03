package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.common.security.util.JwtUtil;
import com.wxy.infra.biz.bo.InfraTokenCacheBO;
import com.wxy.infra.biz.config.InfraTokenProperties;
import com.wxy.infra.biz.mapper.InfraTokenMapper;
import com.wxy.infra.biz.mapper.InfraTokenRefreshMapper;
import com.wxy.infra.biz.po.InfraToken;
import com.wxy.infra.biz.po.InfraTokenRefresh;
import com.wxy.infra.biz.vo.admin.AuthTokenRespVO;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 凭证服务单元测试：验证「Redis 优先、MySQL 权威」的校验链路与端类型隔离。
 *
 * <p>全部用 mock 替代 Redis 与数据库，不依赖任何外部环境；
 * JWT 工具用真实实现，确保签名与解析这一段是真的跑通的。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraTokenServiceImplTest {

    /** 测试用密钥：与 dev 环境一致，长度满足 HS256 要求 */
    private static final String SECRET = "a99c28Ed65114571aa1e4aa9e2c8c813";

    /** access token 有效期（秒） */
    private static final long EXPIRE_SECONDS = 7200L;

    /** Redis 读写工具 */
    @Mock
    private RedisUtil redisUtil;

    /** 访问凭证 Mapper */
    @Mock
    private InfraTokenMapper infraTokenMapper;

    /** 续期凭证 Mapper */
    @Mock
    private InfraTokenRefreshMapper infraTokenRefreshMapper;

    /** 被测服务 */
    private InfraTokenServiceImpl tokenService;

    /** 真实的 JWT 工具 */
    private JwtUtil jwtUtil;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRE_SECONDS);
        tokenService = new InfraTokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(tokenService, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(tokenService, "infraTokenMapper", infraTokenMapper);
        ReflectionTestUtils.setField(tokenService, "infraTokenRefreshMapper", infraTokenRefreshMapper);
        ReflectionTestUtils.setField(tokenService, "infraTokenProperties", new InfraTokenProperties());
    }

    /**
     * 缓存命中时直接返回登录用户，不再查库
     */
    @Test
    @DisplayName("validate：Redis 命中时不回查 MySQL")
    void validateShouldHitCache() {
        String token = jwtUtil.generate(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));
        when(redisUtil.get(anyString(), eq(InfraTokenCacheBO.class)))
                .thenReturn(buildCache(1L, UserTypeEnum.ADMIN, LocalDateTime.now().plusHours(1)));

        LoginUser loginUser = tokenService.validate("Bearer " + token, UserTypeEnum.ADMIN);

        assertThat(loginUser.userId()).isEqualTo(1L);
        assertThat(loginUser.userType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        assertThat(loginUser.username()).isEqualTo("admin");
        verifyNoInteractions(infraTokenMapper);
    }

    /**
     * 缓存未命中时回查 MySQL 并回写缓存，TTL 取剩余有效期
     */
    @Test
    @DisplayName("validate：缓存未命中时回查 MySQL 并回写 Redis")
    void validateShouldFallbackToDatabase() {
        String token = jwtUtil.generate(new LoginUser(2L, UserTypeEnum.ADMIN.getValue(), "infra"));
        when(redisUtil.get(anyString(), eq(InfraTokenCacheBO.class))).thenReturn(null);
        when(infraTokenMapper.selectOne(any())).thenReturn(buildTokenRecord(2L, UserTypeEnum.ADMIN));

        LoginUser loginUser = tokenService.validate(token, UserTypeEnum.ADMIN);

        assertThat(loginUser.userId()).isEqualTo(2L);
        verify(redisUtil).set(anyString(), any(), anyLong(), eq(TimeUnit.SECONDS));
    }

    /**
     * 缓存与库里都没有有效记录（例如已登出、已轮换）时必须拒绝
     */
    @Test
    @DisplayName("validate：缓存与 MySQL 都没有有效记录时抛 401")
    void validateShouldRejectUnknownToken() {
        String token = jwtUtil.generate(new LoginUser(3L, UserTypeEnum.ADMIN.getValue(), "infra"));
        when(redisUtil.get(anyString(), eq(InfraTokenCacheBO.class))).thenReturn(null);
        when(infraTokenMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> tokenService.validate(token, UserTypeEnum.ADMIN))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 伪造或过期的 token 连缓存都不该查
     */
    @Test
    @DisplayName("validate：签名非法的 token 直接抛 401，不查缓存与数据库")
    void validateShouldRejectForgedToken() {
        assertThatThrownBy(() -> tokenService.validate("Bearer not-a-jwt", UserTypeEnum.ADMIN))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(redisUtil);
        verifyNoInteractions(infraTokenMapper);
    }

    /**
     * app 端凭证不能访问 admin 端接口，反之亦然
     */
    @Test
    @DisplayName("validate：端类型不匹配时抛 401")
    void validateShouldRejectMismatchedUserType() {
        String token = jwtUtil.generate(new LoginUser(4L, UserTypeEnum.APP.getValue(), "app-user"));
        when(redisUtil.get(anyString(), eq(InfraTokenCacheBO.class)))
                .thenReturn(buildCache(4L, UserTypeEnum.APP, LocalDateTime.now().plusHours(1)));

        assertThatThrownBy(() -> tokenService.validate(token, UserTypeEnum.ADMIN))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 签发凭证必须同时落库 access 与续期两条记录，并返回带有效期的凭证
     */
    @Test
    @DisplayName("createTokenPair：落库两条凭证并返回访问凭证与续期凭证")
    void createTokenPairShouldPersistBothRecords() {
        AuthTokenRespVO respVO = tokenService.createTokenPair(5L, UserTypeEnum.ADMIN, "infra", "127.0.0.1");

        assertThat(respVO.getAccessToken()).isNotBlank();
        assertThat(respVO.getRefreshToken()).isNotBlank();
        assertThat(respVO.getTokenType()).isEqualTo("Bearer");
        assertThat(respVO.getExpiresIn()).isEqualTo(EXPIRE_SECONDS);
        verify(infraTokenMapper).insert(any(InfraToken.class));
        verify(infraTokenRefreshMapper).insert(any(InfraTokenRefresh.class));
    }

    /**
     * 登出必须同时失效 access 记录与同一会话的续期记录
     */
    @Test
    @DisplayName("revoke：access 与同一会话的续期凭证一并失效")
    void revokeShouldDisableBothTokens() {
        String token = jwtUtil.generate(new LoginUser(6L, UserTypeEnum.ADMIN.getValue(), "infra"));
        InfraToken record = buildTokenRecord(6L, UserTypeEnum.ADMIN);
        record.setRefreshTokenHash("refresh-hash");
        when(infraTokenMapper.selectOne(any())).thenReturn(record);
        when(infraTokenRefreshMapper.selectOne(any())).thenReturn(new InfraTokenRefresh());

        tokenService.revoke("Bearer " + token);

        assertThat(record.getStatus()).isEqualTo(CommonStatusEnum.DISABLED.getValue());
        verify(infraTokenMapper).updateById(record);
        verify(infraTokenRefreshMapper).updateById(any(InfraTokenRefresh.class));
    }

    /**
     * 凭证不存在时登出接口必须幂等
     */
    @Test
    @DisplayName("revoke：凭证不存在时幂等返回")
    void revokeShouldBeIdempotent() {
        String token = jwtUtil.generate(new LoginUser(7L, UserTypeEnum.ADMIN.getValue(), "infra"));
        when(infraTokenMapper.selectOne(any())).thenReturn(null);

        tokenService.revoke("Bearer " + token);

        verify(infraTokenMapper, never()).updateById(any(InfraToken.class));
    }

    /**
     * 构造缓存对象
     *
     * @param userId   用户 ID
     * @param userType 端类型
     * @param expire   过期时间
     * @return 缓存对象
     */
    private InfraTokenCacheBO buildCache(Long userId, UserTypeEnum userType, LocalDateTime expire) {
        InfraTokenCacheBO cache = new InfraTokenCacheBO();
        cache.setUserId(userId);
        cache.setUserType(userType.getValue());
        cache.setUsername("admin");
        cache.setExpireTime(expire);
        cache.setLoginIp("127.0.0.1");
        return cache;
    }

    /**
     * 构造有效凭证记录
     *
     * @param userId   用户 ID
     * @param userType 端类型
     * @return 凭证记录
     */
    private InfraToken buildTokenRecord(Long userId, UserTypeEnum userType) {
        InfraToken record = new InfraToken();
        record.setUserId(userId);
        record.setUserType(userType.getValue());
        record.setUsername("admin");
        record.setStatus(CommonStatusEnum.ENABLED.getValue());
        record.setExpireTime(LocalDateTime.now().plusHours(1));
        return record;
    }
}
