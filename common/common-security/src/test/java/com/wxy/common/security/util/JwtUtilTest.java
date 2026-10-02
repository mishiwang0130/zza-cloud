package com.wxy.common.security.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JWT 签发与解析测试：覆盖正常解析、凭证非法、凭证过期与密钥不合法四种情况。
 *
 * @author wxy
 * @date 2026/10/02
 */
class JwtUtilTest {

    /** 测试用密钥：长度满足 HS256 的 32 字节要求 */
    private static final String SECRET = "zza-cloud-common-security-test-secret-key";

    @Test
    @DisplayName("签发的 token 可以解析出登录用户")
    void shouldGenerateAndParseToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600L);

        String token = jwtUtil.generate(new LoginUser(1001L, UserTypeEnum.ADMIN.getValue(), "admin"));
        LoginUser loginUser = jwtUtil.parse(token);

        assertThat(loginUser.userId()).isEqualTo(1001L);
        assertThat(loginUser.userType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        assertThat(loginUser.username()).isEqualTo("admin");
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("解析时兼容 Bearer 前缀")
    void shouldAcceptBearerPrefix() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600L);

        String token = jwtUtil.generate(new LoginUser(1001L, UserTypeEnum.APP.getValue(), "member"));

        assertThat(jwtUtil.parse("Bearer " + token).userId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("凭证非法时抛未登录异常")
    void shouldRejectIllegalToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600L);

        assertThatThrownBy(() -> jwtUtil.parse("not-a-token")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> jwtUtil.parse(null)).isInstanceOf(UnauthorizedException.class);
        assertThat(jwtUtil.isValid("not-a-token")).isFalse();
    }

    @Test
    @DisplayName("凭证过期时抛未登录异常")
    void shouldRejectExpiredToken() throws InterruptedException {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 1L);
        String token = jwtUtil.generate(new LoginUser(1001L, UserTypeEnum.ADMIN.getValue(), "admin"));

        Thread.sleep(1500L);

        assertThatThrownBy(() -> jwtUtil.parse(token)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("密钥缺失或长度不足时启动即失败")
    void shouldRejectIllegalSecret() {
        assertThatThrownBy(() -> new JwtUtil("short", 3600L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtUtil(null, 3600L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtUtil(SECRET, 0L)).isInstanceOf(IllegalStateException.class);
    }
}
