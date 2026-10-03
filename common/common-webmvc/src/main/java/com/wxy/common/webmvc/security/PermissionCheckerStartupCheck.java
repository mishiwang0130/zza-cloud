package com.wxy.common.webmvc.security;

import com.wxy.common.core.security.PermissionChecker;
import com.wxy.common.core.security.RequiresPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 权限实现启动检查：一旦有接口标注了 {@link RequiresPermission}，服务就必须提供 {@link PermissionChecker}。
 *
 * <p>为什么放在启动阶段：权限实现缺失属于配置错误，等接口被调用时才报错会拖到测试甚至线上；
 * 而且失败方式是「拒绝」，很容易被误当成权限没配好，排查成本高。启动直接失败，信息里带上
 * 全部受影响的接口，一眼就能定位。
 *
 * <p>反过来，一个接口都没标注的服务不需要提供实现——「不需要权限控制」这件事本来就写在代码里了。
 *
 * @author wxy
 * @date 2026/10/03
 */
public class PermissionCheckerStartupCheck implements ApplicationRunner {

    /** 控制器映射：启动后可以拿到所有接口与其方法 */
    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider;

    /** 权限校验器：服务提供则跳过检查 */
    private final ObjectProvider<PermissionChecker> permissionCheckerProvider;

    /**
     * 构造启动检查
     *
     * @param handlerMappingProvider     控制器映射提供者
     * @param permissionCheckerProvider  权限校验器提供者
     */
    public PermissionCheckerStartupCheck(ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider,
                                         ObjectProvider<PermissionChecker> permissionCheckerProvider) {
        this.handlerMappingProvider = handlerMappingProvider;
        this.permissionCheckerProvider = permissionCheckerProvider;
    }

    /**
     * 校验「有权限注解的接口」都有权限实现
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (permissionCheckerProvider.getIfAvailable() != null) {
            return;
        }
        RequestMappingHandlerMapping handlerMapping = handlerMappingProvider.getIfAvailable();
        if (handlerMapping == null) {
            return;
        }
        List<String> annotatedEndpoints = collectAnnotatedEndpoints(handlerMapping.getHandlerMethods());
        if (annotatedEndpoints.isEmpty()) {
            return;
        }
        throw new IllegalStateException("以下接口标注了 @RequiresPermission，但服务未提供 PermissionChecker 实现，"
                + "无法校验接口权限：" + annotatedEndpoints
                + "；请实现 com.wxy.common.core.security.PermissionChecker"
                + "（可参考 infra 的 InfraPermissionChecker），或去掉这些接口上的权限注解");
    }

    /**
     * 收集所有标注了权限注解的接口
     *
     * @param handlerMethods 控制器方法映射
     * @return 接口描述列表，形如 {@code GET /admin-api/user/page}；没有时返回空列表
     */
    private List<String> collectAnnotatedEndpoints(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        List<String> endpoints = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            if (!hasRequiresPermission(entry.getValue())) {
                continue;
            }
            endpoints.add(describe(entry.getKey(), entry.getValue()));
        }
        return endpoints;
    }

    /**
     * 判断控制器方法是否要求权限（方法或所在类标注即可）
     *
     * @param handlerMethod 控制器方法
     * @return 需要权限时返回 true
     */
    private boolean hasRequiresPermission(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequiresPermission.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequiresPermission.class);
    }

    /**
     * 拼接口描述，便于在报错信息里直接定位
     *
     * @param mapping       接口映射
     * @param handlerMethod 控制器方法
     * @return 接口描述
     */
    private String describe(RequestMappingInfo mapping, HandlerMethod handlerMethod) {
        Set<String> patterns = mapping.getPathPatternsCondition() != null
                ? mapping.getPathPatternsCondition().getPatternValues()
                : Set.of();
        return mapping.getMethodsCondition().getMethods() + " " + patterns + " -> "
                + handlerMethod.getShortLogMessage();
    }
}
