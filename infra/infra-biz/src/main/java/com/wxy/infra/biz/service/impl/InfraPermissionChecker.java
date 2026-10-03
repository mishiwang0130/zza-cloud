package com.wxy.infra.biz.service.impl;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.security.PermissionChecker;
import com.wxy.infra.biz.service.InfraPermissionService;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * infra 的权限校验实现：把公共 SPI 接到本服务的权限数据上。
 *
 * <p>权限数据来自「用户 → 角色 → 菜单」，由 {@link InfraPermissionService} 提供（缓存优先、回查 MySQL）；
 * 两条 infra 自己的业务规则也放在这里，公共层不认识它们：
 * <ol>
 *   <li>未区分端的身份（端类型为 null）与 app 端身份不做后台权限校验——
 *       菜单权限是管理后台的概念，app 端权限体系接入前一律拒绝；</li>
 *   <li>超级管理员直接放行，不查权限集合。</li>
 * </ol>
 *
 * <p>其他服务要复用这套权限数据时，走 infra 对外发布的 api 模块查询（由调用方自己实现
 * {@code PermissionChecker}），公共拦截器不需要改。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Service
public class InfraPermissionChecker implements PermissionChecker {

    /** 权限服务 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /**
     * 判断登录用户是否拥有其中任意一个权限
     *
     * @param loginUser   当前登录用户
     * @param permissions 接口要求的权限标识
     * @return 拥有任意一个权限时返回 true
     */
    @Override
    public boolean hasAnyPermission(LoginUser loginUser, Collection<String> permissions) {
        if (!UserTypeEnum.ADMIN.getValue().equals(loginUser.userType())) {
            // 管理后台的菜单权限不适用于其他端，app 端权限体系接入前按无权限处理
            return false;
        }
        if (infraPermissionService.isSuperAdmin(loginUser.userId())) {
            return true;
        }
        Set<String> userPermissions = infraPermissionService.getPermissions(loginUser.userId());
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }
}
