package com.wxy.infra.biz.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.webmvc.config.WebProperties;
import com.wxy.infra.biz.config.InfraSecurityProperties;
import com.wxy.infra.biz.service.InfraTokenService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

/**
 * 凭证拦截器单元测试：免登录注解、令牌来源、端类型判定与模拟登录。
 *
 * <p>重点锁住端类型判定的边界：只认约定前缀，判不出来时**不比对端类型**，
 * 而不是兜底成某一端（曾经写成「不是 admin 就当 app」，会把新增端静默按 app 校验）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    /** 凭证服务 */
    @Mock
    private InfraTokenService infraTokenService;

    /**
     * 清理线程上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 标注 {@link PermitAll} 的接口免登录，连凭证服务都不该调用
     */
    @Test
    @DisplayName("preHandle：标注 PermitAll 的接口直接放行")
    void shouldPassPermitAllEndpoint() throws NoSuchMethodException {
        boolean handled = newInterceptor(new InfraSecurityProperties())
                .preHandle(mock(HttpServletRequest.class), null, buildHandler("login"));

        assertThat(handled).isTrue();
        verifyNoInteractions(infraTokenService);
        assertThat(UserContextHolder.get()).isNull();
    }

    /**
     * 没有携带任何令牌时直接 401，不去查缓存与数据库
     */
    @Test
    @DisplayName("preHandle：没有令牌时抛 401")
    void shouldRejectWithoutToken() throws NoSuchMethodException {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThatThrownBy(() -> newInterceptor(new InfraSecurityProperties())
                .preHandle(request, null, buildHandler("list")))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(infraTokenService);
    }

    /**
     * admin 端接口要求 admin 端凭证
     */
    @Test
    @DisplayName("preHandle：/admin-api 前缀要求 admin 端凭证")
    void shouldRequireAdminUserType() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer token", "/admin-api/user/page");
        when(infraTokenService.validate("token", UserTypeEnum.ADMIN))
                .thenReturn(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));

        assertThat(newInterceptor(new InfraSecurityProperties())
                .preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(1L);
    }

    /**
     * app 端接口要求 app 端凭证
     */
    @Test
    @DisplayName("preHandle：/app-api 前缀要求 app 端凭证")
    void shouldRequireAppUserType() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer token", "/app-api/user/getUserInfo");
        when(infraTokenService.validate("token", UserTypeEnum.APP))
                .thenReturn(new LoginUser(2L, UserTypeEnum.APP.getValue(), "app-user"));

        assertThat(newInterceptor(new InfraSecurityProperties())
                .preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserType()).isEqualTo(UserTypeEnum.APP.getValue());
    }

    /**
     * 前缀不在约定范围内时不比对端类型（传 null），而不是兜底成 app 端
     */
    @Test
    @DisplayName("preHandle：无法判定端类型时不比对端类型，不兜底成 app 端")
    void shouldNotFallbackUserType() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer token", "/ws/chat");
        when(infraTokenService.validate("token", null))
                .thenReturn(new LoginUser(3L, UserTypeEnum.ADMIN.getValue(), "admin"));

        assertThat(newInterceptor(new InfraSecurityProperties())
                .preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(3L);
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
        when(infraTokenService.validate("param-token", UserTypeEnum.ADMIN))
                .thenReturn(new LoginUser(4L, UserTypeEnum.ADMIN.getValue(), "admin"));

        assertThat(newInterceptor(new InfraSecurityProperties())
                .preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(4L);
    }

    /**
     * 模拟登录默认关闭：令牌校验失败就是 401
     */
    @Test
    @DisplayName("preHandle：模拟登录关闭时，无效令牌抛 401")
    void shouldRejectWhenMockDisabled() throws NoSuchMethodException {
        HttpServletRequest request = requestWithHeader("Bearer test9", "/admin-api/user/page");
        when(infraTokenService.validate("test9", UserTypeEnum.ADMIN))
                .thenThrow(new UnauthorizedException());

        assertThatThrownBy(() -> newInterceptor(new InfraSecurityProperties())
                .preHandle(request, null, buildHandler("list")))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 模拟登录打开时，{@code mockSecret + 用户 ID} 的令牌直接当成该用户
     */
    @Test
    @DisplayName("preHandle：模拟登录打开时，mock 令牌放行并写入上下文")
    void shouldPassMockToken() throws NoSuchMethodException {
        InfraSecurityProperties securityProperties = new InfraSecurityProperties();
        securityProperties.setMockEnable(true);
        HttpServletRequest request = requestWithHeader("Bearer test9", "/admin-api/user/page");
        when(infraTokenService.validate("test9", UserTypeEnum.ADMIN))
                .thenThrow(new UnauthorizedException());

        assertThat(newInterceptor(securityProperties).preHandle(request, null, buildHandler("list"))).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(9L);
        assertThat(UserContextHolder.getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
    }

    /**
     * 构造被测拦截器
     *
     * @param securityProperties 安全配置
     * @return 拦截器
     */
    private AuthInterceptor newInterceptor(InfraSecurityProperties securityProperties) {
        return new AuthInterceptor(infraTokenService, new WebProperties(), securityProperties);
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
