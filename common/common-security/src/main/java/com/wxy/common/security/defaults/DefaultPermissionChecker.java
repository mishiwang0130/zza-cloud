package com.wxy.common.security.defaults;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.security.PermissionChecker;
import com.wxy.infra.api.client.InfraPermissionClient;
import com.wxy.infra.api.dto.PermissionCheckReqDTO;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认的权限校验实现：调 infra 判断用户是否拥有指定权限。
 *
 * <p>权限数据（用户 → 角色 → 菜单）只有 infra 有，所以默认实现就是一次远程判断；
 * 服务自己维护权限数据时，定义同类型的 {@code PermissionChecker} Bean 覆盖即可。
 *
 * <p>远程调用的熔断降级在 {@link InfraPermissionClient} 的 {@code fallbackFactory} 里：
 * 调用失败或被熔断时返回「无权限」，权限校验不能因为依赖不可用而放行。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
public class DefaultPermissionChecker implements PermissionChecker {

    /** infra 的权限服务：判断用户是否拥有指定权限 */
    private final InfraPermissionClient infraPermissionClient;

    /**
     * 构造校验器
     *
     * @param infraPermissionClient infra 权限服务客户端
     */
    public DefaultPermissionChecker(InfraPermissionClient infraPermissionClient) {
        this.infraPermissionClient = infraPermissionClient;
    }

    /**
     * 判断登录用户是否拥有其中任意一个权限
     *
     * @param loginUser   当前登录用户
     * @param permissions 接口要求的权限标识
     * @return 拥有任意一个权限时返回 true
     */
    @Override
    public boolean hasAnyPermission(LoginUser loginUser, Collection<String> permissions) {
        PermissionCheckReqDTO reqDTO = new PermissionCheckReqDTO();
        reqDTO.setUserId(loginUser.userId());
        reqDTO.setUserType(loginUser.userType());
        reqDTO.setPermissions(permissions);
        // 远端降级时返回的是 false（成功响应），调用失败时才由 requireData() 抛异常
        return Boolean.TRUE.equals(infraPermissionClient.hasAnyPermission(reqDTO).requireData());
    }
}
