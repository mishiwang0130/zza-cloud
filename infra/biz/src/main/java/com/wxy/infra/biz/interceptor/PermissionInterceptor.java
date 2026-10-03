package com.wxy.infra.biz.interceptor;

import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.infra.biz.annotation.RequiresPermission;
import com.wxy.infra.biz.service.InfraPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口权限拦截器：按 {@code @RequiresPermission} 校验当前用户的权限标识。
 *
 * <p>端无关：没有注解的接口直接放行（app 端的控制器在权限体系接入前不加注解即可正常工作）；
 * 标注了注解的接口，只有「超级管理员」或「拥有任一所需权限」的用户才放行。
 *
 * <p>app 端目前没有角色权限表，APP 身份命中权限注解时一律拒绝（无权限），
 * 等 app 端权限体系落地后在这里扩展取权限的来源即可。
 *
 * @author wxy
 * @date 2026/10/03
 */
public class PermissionInterceptor implements HandlerInterceptor {

    /** 权限服务 */
    private final InfraPermissionService permissionService;

    /**
     * 构造拦截器
     *
     * @param permissionService 权限服务
     */
    public PermissionInterceptor(InfraPermissionService permissionService) {
        this.permissionService = permissionService;
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
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 静态资源等非控制器处理器不带权限语义
            return true;
        }
        RequiresPermission requires = resolveAnnotation(handlerMethod);
        if (requires == null) {
            return true;
        }
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            // 正常流程下凭证拦截器已经写入上下文，这里兜底避免「未登录却按有权限处理」
            throw new UnauthorizedException();
        }
        if (!UserTypeEnum.ADMIN.getValue().equals(UserContextHolder.getUserType())) {
            throw new BizException(CommonErrorConstant.FORBIDDEN);
        }
        if (permissionService.isSuperAdmin(userId)) {
            return true;
        }
        Set<String> perms = permissionService.getPermissions(userId);
        for (String required : requires.value()) {
            if (perms.contains(required)) {
                return true;
            }
        }
        throw new BizException(CommonErrorConstant.FORBIDDEN);
    }

    /**
     * 取方法上的权限注解，方法没写再看类上的
     *
     * @param handlerMethod 控制器方法
     * @return 权限注解，未标注时返回 null
     */
    private RequiresPermission resolveAnnotation(HandlerMethod handlerMethod) {
        RequiresPermission requires = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (requires != null) {
            return requires;
        }
        return handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
    }
}
