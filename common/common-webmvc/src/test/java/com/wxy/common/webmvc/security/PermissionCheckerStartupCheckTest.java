package com.wxy.common.webmvc.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wxy.common.core.security.PermissionChecker;
import com.wxy.common.core.security.RequiresPermission;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 权限实现启动检查单元测试：有权限注解就必须有 {@code PermissionChecker}，否则启动失败。
 *
 * @author wxy
 * @date 2026/10/03
 */
class PermissionCheckerStartupCheckTest {

    /**
     * 没有任何权限注解时不需要实现
     */
    @Test
    @DisplayName("run：没有权限注解时放行")
    void shouldPassWhenNoAnnotatedEndpoint() {
        DefaultListableBeanFactory beanFactory = beanFactoryWithHandlers(false, false);

        assertThatCode(() -> newStartupCheck(beanFactory).run(null)).doesNotThrowAnyException();
    }

    /**
     * 有权限注解 + 有实现时正常启动
     */
    @Test
    @DisplayName("run：有权限注解且有实现时放行")
    void shouldPassWhenCheckerPresent() {
        DefaultListableBeanFactory beanFactory = beanFactoryWithHandlers(true, false);
        beanFactory.registerSingleton("permissionChecker",
                (PermissionChecker) (loginUser, permissions) -> true);

        assertThatCode(() -> newStartupCheck(beanFactory).run(null)).doesNotThrowAnyException();
    }

    /**
     * 有权限注解但没有实现时启动失败，报错信息里带出受影响的接口
     */
    @Test
    @DisplayName("run：有权限注解但没有实现时启动失败")
    void shouldFailWhenCheckerMissing() {
        DefaultListableBeanFactory beanFactory = beanFactoryWithHandlers(true, false);

        assertThatThrownBy(() -> newStartupCheck(beanFactory).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PermissionChecker")
                .hasMessageContaining("/admin-api/user/page");
    }

    /**
     * 类上标注权限注解也算需要实现
     */
    @Test
    @DisplayName("run：类上标注权限注解也算需要实现")
    void shouldFailWhenClassAnnotated() {
        DefaultListableBeanFactory beanFactory = beanFactoryWithHandlers(false, true);

        assertThatThrownBy(() -> newStartupCheck(beanFactory).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PermissionChecker");
    }

    /**
     * 构造启动检查
     *
     * @param beanFactory Bean 工厂，用于提供控制器映射与权限校验器
     * @return 启动检查
     */
    private PermissionCheckerStartupCheck newStartupCheck(DefaultListableBeanFactory beanFactory) {
        return new PermissionCheckerStartupCheck(
                beanFactory.getBeanProvider(RequestMappingHandlerMapping.class),
                beanFactory.getBeanProvider(PermissionChecker.class));
    }

    /**
     * 构造带控制器映射的 Bean 工厂
     *
     * @param methodAnnotated 是否注册一个方法级带权限注解的接口
     * @param classAnnotated  是否注册一个类级带权限注解的接口
     * @return Bean 工厂
     */
    private DefaultListableBeanFactory beanFactoryWithHandlers(boolean methodAnnotated, boolean classAnnotated) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = new LinkedHashMap<>();
        if (methodAnnotated) {
            handlerMethods.put(RequestMappingInfo.paths("/admin-api/user/page").build(),
                    buildHandler(new MethodAnnotatedController(), "page"));
        }
        if (classAnnotated) {
            handlerMethods.put(RequestMappingInfo.paths("/admin-api/role/page").build(),
                    buildHandler(new ClassAnnotatedController(), "page"));
        }
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandlerMethods()).thenReturn(handlerMethods);
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("requestMappingHandlerMapping", handlerMapping);
        return beanFactory;
    }

    /**
     * 构造控制器方法处理器
     *
     * @param controller 控制器实例
     * @param methodName 方法名
     * @return 处理器
     */
    private HandlerMethod buildHandler(Object controller, String methodName) {
        try {
            return new HandlerMethod(controller, controller.getClass().getMethod(methodName));
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 方法级权限注解的测试控制器
     */
    private static class MethodAnnotatedController {

        /**
         * 需要权限的接口
         */
        @RequiresPermission("test:user:query")
        public void page() {
        }
    }

    /**
     * 类级权限注解的测试控制器
     */
    @RequiresPermission("test:role:query")
    private static class ClassAnnotatedController {

        /**
         * 继承类上的权限要求
         */
        public void page() {
        }
    }
}
