package com.wxy.common.webmvc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.security.PermissionChecker;
import com.wxy.common.core.security.RequiresPermission;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

/**
 * 公共权限拦截器单元测试：注解解析、未登录拒绝、校验器裁决与缺失实现时的兜底。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class PermissionInterceptorTest {

    /** 测试用权限标识 */
    private static final String TEST_PERMISSION = "test:user:create";

    /** 权限校验器：各服务自己的实现 */
    @Mock
    private PermissionChecker permissionChecker;

    /**
     * 清理线程上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 没有权限注解的方法直接放行，且不查权限
     */
    @Test
    @DisplayName("preHandle：无权限注解时放行且不查权限")
    void shouldPassWithoutAnnotation() throws NoSuchMethodException {
        assertThat(new PermissionInterceptor(permissionChecker)
                .preHandle(null, null, buildHandler("plain"))).isTrue();
        verifyNoInteractions(permissionChecker);
    }

    /**
     * 校验器判定有权限时放行
     */
    @Test
    @DisplayName("preHandle：校验器判定有权限时放行")
    void shouldPassWhenCheckerAllows() throws NoSuchMethodException {
        UserContextHolder.set(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));
        when(permissionChecker.hasAnyPermission(any(LoginUser.class), any(Collection.class))).thenReturn(true);

        assertThat(new PermissionInterceptor(permissionChecker)
                .preHandle(null, null, buildHandler("create"))).isTrue();
    }

    /**
     * 校验器判定无权限时按无权限拒绝（HTTP 200 + 无权限错误码）
     */
    @Test
    @DisplayName("preHandle：校验器判定无权限时抛无权限")
    void shouldRejectWhenCheckerDenies() throws NoSuchMethodException {
        UserContextHolder.set(new LoginUser(2L, UserTypeEnum.ADMIN.getValue(), "infra"));
        when(permissionChecker.hasAnyPermission(any(LoginUser.class), eq(java.util.List.of(TEST_PERMISSION))))
                .thenReturn(false);

        assertThatThrownBy(() -> new PermissionInterceptor(permissionChecker)
                .preHandle(null, null, buildHandler("create")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorConstant.FORBIDDEN.code()));
    }

    /**
     * 上下文为空（拦截器配置被改错）时按未登录拒绝，而不是匿名放行
     */
    @Test
    @DisplayName("preHandle：未登录时抛 401")
    void shouldRejectAnonymous() throws NoSuchMethodException {
        assertThatThrownBy(() -> new PermissionInterceptor(permissionChecker)
                .preHandle(null, null, buildHandler("create")))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 服务没提供校验器时按无权限拒绝（正常情况下启动阶段就会失败）
     */
    @Test
    @DisplayName("preHandle：未提供校验器时按无权限拒绝")
    void shouldRejectWhenCheckerMissing() throws NoSuchMethodException {
        UserContextHolder.set(new LoginUser(3L, UserTypeEnum.ADMIN.getValue(), "infra"));

        assertThatThrownBy(() -> new PermissionInterceptor(null)
                .preHandle(null, null, buildHandler("create")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorConstant.FORBIDDEN.code()));
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
        @RequiresPermission(TEST_PERMISSION)
        public void create() {
        }

        /**
         * 不带权限注解的方法
         */
        public void plain() {
        }
    }
}
