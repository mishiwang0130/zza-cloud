package com.wxy.infra.biz.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.infra.biz.annotation.RequiresPermission;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.service.InfraPermissionService;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

/**
 * 权限拦截器单元测试：覆盖「无注解放行、超管放行、有权限放行、无权限拒绝、app 身份命中注解拒绝」。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class PermissionInterceptorTest {

    /** 权限服务 */
    @Mock
    private InfraPermissionService permissionService;

    /** 被测拦截器 */
    private PermissionInterceptor interceptor;

    /**
     * 装配拦截器
     */
    @BeforeEach
    void setUp() {
        interceptor = new PermissionInterceptor(permissionService);
    }

    /**
     * 清理线程上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 没有权限注解的方法直接放行，且不查权限数据
     */
    @Test
    @DisplayName("preHandle：无权限注解时放行且不查权限")
    void shouldPassWithoutAnnotation() throws NoSuchMethodException {
        assertThat(interceptor.preHandle(null, null, buildHandler("plain"))).isTrue();
        verifyNoInteractions(permissionService);
    }

    /**
     * 超级管理员跳过权限校验
     */
    @Test
    @DisplayName("preHandle：超级管理员直接放行")
    void shouldPassForSuperAdmin() throws NoSuchMethodException {
        UserContextHolder.set(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));
        when(permissionService.isSuperAdmin(1L)).thenReturn(true);

        assertThat(interceptor.preHandle(null, null, buildHandler("create"))).isTrue();
    }

    /**
     * 拥有接口所需权限标识时放行
     */
    @Test
    @DisplayName("preHandle：拥有权限标识时放行")
    void shouldPassWhenPermissionMatched() throws NoSuchMethodException {
        UserContextHolder.set(new LoginUser(2L, UserTypeEnum.ADMIN.getValue(), "infra"));
        when(permissionService.isSuperAdmin(2L)).thenReturn(false);
        when(permissionService.getPermissions(2L)).thenReturn(Set.of(InfraPermissionConstant.USER_CREATE));

        assertThat(interceptor.preHandle(null, null, buildHandler("create"))).isTrue();
    }

    /**
     * 没有所需权限时按「无权限」拒绝（业务错误码，HTTP 200）
     */
    @Test
    @DisplayName("preHandle：缺少权限标识时抛无权限")
    void shouldRejectWhenPermissionMissing() throws NoSuchMethodException {
        UserContextHolder.set(new LoginUser(3L, UserTypeEnum.ADMIN.getValue(), "infra"));
        when(permissionService.isSuperAdmin(3L)).thenReturn(false);
        when(permissionService.getPermissions(3L)).thenReturn(Set.of());

        assertThatThrownBy(() -> interceptor.preHandle(null, null, buildHandler("create")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorConstant.FORBIDDEN.code()));
    }

    /**
     * app 端身份访问带权限注解的接口一律拒绝：app 端权限体系尚未接入，不能默认放行
     */
    @Test
    @DisplayName("preHandle：app 端身份命中权限注解时拒绝")
    void shouldRejectAppUserType() throws NoSuchMethodException {
        UserContextHolder.set(new LoginUser(4L, UserTypeEnum.APP.getValue(), "app-user"));

        assertThatThrownBy(() -> interceptor.preHandle(null, null, buildHandler("create")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorConstant.FORBIDDEN.code()));
    }

    /**
     * 上下文为空（拦截器配置被改错）时按未登录拒绝，而不是匿名放行
     */
    @Test
    @DisplayName("preHandle：未登录时抛 401")
    void shouldRejectAnonymous() throws NoSuchMethodException {
        assertThatThrownBy(() -> interceptor.preHandle(null, null, buildHandler("create")))
                .isInstanceOf(UnauthorizedException.class);
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
     * 测试用控制器：一个方法带权限注解，一个不带
     */
    private static class SampleController {

        /**
         * 带权限注解的方法
         */
        @RequiresPermission(InfraPermissionConstant.USER_CREATE)
        public void create() {
        }

        /**
         * 不带权限注解的方法
         */
        public void plain() {
        }
    }
}
