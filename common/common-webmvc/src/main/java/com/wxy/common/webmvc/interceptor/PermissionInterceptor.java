package com.wxy.common.webmvc.interceptor;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.security.PermissionChecker;
import com.wxy.common.core.security.RequiresPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口权限拦截器：按 {@link RequiresPermission} 注解校验当前用户是否有权限访问该接口。
 *
 * <p>端无关，也不认识任何业务数据：权限从哪来由各服务的 {@link PermissionChecker} 决定
 * （infra 自己查「用户 → 角色 → 菜单」，其他服务可以调 infra 查询）。
 * 超管放行这类业务规则也放在服务的实现里，公共层不做特例。
 *
 * <p>没有注解的接口直接放行；标了注解但服务没提供 {@code PermissionChecker} 时按无权限拒绝
 * （正常流程下启动阶段就会失败，见 {@code PermissionCheckerStartupCheck}，这里是最后一道兜底）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
public class PermissionInterceptor implements HandlerInterceptor {

    /** 权限校验器：可以为 null，由启动检查保证「有注解就必须有实现」 */
    private final PermissionChecker permissionChecker;

    /**
     * 构造拦截器
     *
     * @param permissionChecker 权限校验器，可以为 null
     */
    public PermissionInterceptor(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    /**
     * 校验接口权限
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  处理器
     * @return true 表示放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequiresPermission requires = resolveAnnotation(handler);
        if (requires == null) {
            return true;
        }
        LoginUser loginUser = UserContextHolder.get();
        if (loginUser == null) {
            // 正常流程下凭证拦截器已经写入上下文，这里兜底避免「未登录却按有权限处理」
            throw new UnauthorizedException();
        }
        if (permissionChecker == null) {
            log.error("接口 {} 标注了 @RequiresPermission 但服务未提供 PermissionChecker 实现，按无权限拒绝",
                    handler instanceof HandlerMethod handlerMethod ? handlerMethod.getMethod() : handler);
            throw new BizException(CommonErrorConstant.FORBIDDEN);
        }
        if (permissionChecker.hasAnyPermission(loginUser, List.of(requires.value()))) {
            return true;
        }
        throw new BizException(CommonErrorConstant.FORBIDDEN);
    }

    /**
     * 取方法上的权限注解，方法没写再看类上的
     *
     * @param handler 处理器
     * @return 权限注解，未标注或不是控制器方法时返回 null
     */
    private RequiresPermission resolveAnnotation(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }
        RequiresPermission requires = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (requires != null) {
            return requires;
        }
        return handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
    }
}
