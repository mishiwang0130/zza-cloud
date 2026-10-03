package com.wxy.common.webmvc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.constant.CommonConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.common.webmvc.config.SecurityProperties;
import com.wxy.common.webmvc.config.WebProperties;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

/**
 * 公共凭证拦截器单元测试：免登录声明、令牌来源、端类型比对与模拟登录。
 *
 * <p>这层是给所有业务服务复用的，所以要锁死两个安全默认值：
 * 未声明免登录的一律要求令牌；端类型判不出来时**不比对**，而不是兜底成某一端。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class TokenAuthInterceptorTest {

    /** 令牌校验器：各服务自己的实现 */
    @Mock
    private TokenValidator tokenValidator;

    /**
     * 清理线程上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 标注 {@link PermitAll} 的接口免登录，连校验器都不该调用
     */
    @Test
    @DisplayName("preHandle：标注 PermitAll 的接口直接放行")
    void shouldPassPermitAllEndpoint() throws NoSuchMethodException {
        boolean handled = newInterceptor(new SecurityProperties())
                .preHandle(mock(HttpServletRequest.class), null, buildHandler("login"));

        assertThat(handled).isTrue();
        verifyNoInteractions(tokenValidator);
    }

    /**
     * 命中配置白名单的整片路径免登录（OpenAPI 分组、服务内部接口等）
     */
    @Test
    @DisplayName("preHandle：命中 yml 白名单的路径直接放行")
    void shouldPassPermitAllUrl() throws NoSuchMethodException {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setPermitAllUrls(List.of("/internal-api/**"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/internal-api/auth/check");

        assertThat(newInterceptor(securityProperties).preHandle(request, null, buildHandler("list"))).isTrue();
        verifyNoInteractions(tokenValidator);
    }

    /**
     * 没有携带令牌时直接 401，不去调用校验器
     */
    @Test
    @DisplayName("preHandle：没有令牌时抛 401")
    void shouldRejectWithoutToken() throws NoSuchMethodException {
        assertThatThrownBy(() -> newInterceptor(new SecurityProperties())
                .preHandle(mock(HttpServletRequest.class), null, buildHandler("list")))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(tokenValidator);
    }

    /**
     * 需要登录的接口：校验通过后把身份写入上下文
     */
    @Test
    @DisplayName("preHandle：校验通过后写入登录上下文")
    void shouldWriteLoginUser() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer token", "/admin-api/user/page");
        when(tokenValidator.validate("token"))
                .thenReturn(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));

        assertThat(newInterceptor(new SecurityProperties()).preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(1L);
    }

    /**
     * app 端令牌不能访问管理后台接口
     */
    @Test
    @DisplayName("preHandle：端类型与接口前缀不匹配时抛 401")
    void shouldRejectMismatchedUserType() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer token", "/admin-api/user/page");
        when(tokenValidator.validate("token"))
                .thenReturn(new LoginUser(2L, UserTypeEnum.APP.getValue(), "app-user"));

        assertThatThrownBy(() -> newInterceptor(new SecurityProperties())
                .preHandle(request, null, buildHandler("list")))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 没有端前缀的接口只校验令牌、不比对端类型，而不是兜底成某一端
     */
    @Test
    @DisplayName("preHandle：无法判定端类型时不比对端类型")
    void shouldSkipUserTypeCheckWithoutPrefix() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer token", "/internal-api/auth/check");
        when(tokenValidator.validate("token"))
                .thenReturn(new LoginUser(3L, UserTypeEnum.ADMIN.getValue(), "admin"));

        assertThat(newInterceptor(new SecurityProperties()).preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(3L);
    }

    /**
     * 实现返回的端类型为 null 表示该服务不区分端（例如放行实现），此时跳过端类型比对
     */
    @Test
    @DisplayName("preHandle：实现不区分端时跳过端类型比对")
    void shouldSkipUserTypeCheckWhenIdentityHasNoUserType() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer token", "/admin-api/user/page");
        when(tokenValidator.validate("token"))
                .thenReturn(new LoginUser(CommonConstant.SYSTEM_USER_ID, null, "anonymous"));

        assertThat(newInterceptor(new SecurityProperties()).preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(CommonConstant.SYSTEM_USER_ID);
    }

    /**
     * WebSocket / SSE 场景：令牌可以放在 {@code ?token=} 请求参数里
     */
    @Test
    @DisplayName("preHandle：支持从请求参数取令牌")
    void shouldResolveTokenFromParameter() throws NoSuchMethodException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/admin-api/ws/connect");
        when(request.getParameter("token")).thenReturn("param-token");
        when(tokenValidator.validate("param-token"))
                .thenReturn(new LoginUser(4L, UserTypeEnum.ADMIN.getValue(), "admin"));

        assertThat(newInterceptor(new SecurityProperties()).preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(4L);
    }

    /**
     * 模拟登录默认关闭：校验失败就是 401
     */
    @Test
    @DisplayName("preHandle：模拟登录关闭时，无效令牌抛 401")
    void shouldRejectWhenMockDisabled() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer test9", "/admin-api/user/page");
        when(tokenValidator.validate("test9")).thenThrow(new UnauthorizedException());

        assertThatThrownBy(() -> newInterceptor(new SecurityProperties())
                .preHandle(request, null, buildHandler("list")))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 模拟登录打开时，{@code mockSecret + 用户 ID} 的令牌直接当成该用户
     */
    @Test
    @DisplayName("preHandle：模拟登录打开时，mock 令牌放行并写入上下文")
    void shouldPassMockToken() throws NoSuchMethodException {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setMockEnable(true);
        HttpServletRequest request = requestWithHeader("Bearer test9", "/admin-api/user/page");
        when(tokenValidator.validate("test9")).thenThrow(new UnauthorizedException());

        assertThat(newInterceptor(securityProperties).preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(9L);
        assertThat(UserContextHolder.getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
    }

    /**
     * 构造被测拦截器
     *
     * @param securityProperties 鉴权配置
     * @return 拦截器
     */
    private TokenAuthInterceptor newInterceptor(SecurityProperties securityProperties) {
        return new TokenAuthInterceptor(tokenValidator, new WebProperties(), securityProperties);
    }

    /**
     * 构造带 Authorization 头与请求路径的请求
     *
     * @param authorization 请求头原值
     * @param uri           请求路径
     * @return 请求
     */
    private HttpServletRequest requestWithHeader(String authorization, String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getHeader(HeaderConstant.AUTHORIZATION)).thenReturn(authorization);
        return request;
    }

    /**
     * 构造控制器方法处理器
     *
     * @param methodName 方法名
     * @return 处理器
     * @throws NoSuchMethodException 方法不存在
     */
    private HandlerMethod buildHandler(String methodName) throws NoSuchMethodException {
        SampleController controller = new SampleController();
        return new HandlerMethod(controller, SampleController.class.getMethod(methodName));
    }

    /**
     * 测试用控制器：一个方法免登录，一个方法需要凭证
     */
    private static class SampleController {

        /**
         * 免登录接口
         */
        @PermitAll
        public void login() {
        }

        /**
         * 需要凭证的接口
         */
        public void list() {
        }
    }
}
