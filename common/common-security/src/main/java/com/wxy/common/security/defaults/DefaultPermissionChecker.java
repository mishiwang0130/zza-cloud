package com.wxy.common.security.defaults;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
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
 * <p>远程调用带 Sentinel 熔断降级，且降级一律返回**无权限**：权限校验不能因为依赖不可用就放行。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
public class DefaultPermissionChecker implements PermissionChecker {

    /** Sentinel 资源名：配置熔断/限流规则时用它定位 */
    public static final String SENTINEL_RESOURCE = "defaultPermissionChecker:hasAnyPermission";

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
    @SentinelResource(value = SENTINEL_RESOURCE,
            blockHandler = "hasAnyPermissionBlocked",
            fallback = "hasAnyPermissionFailed")
    public boolean hasAnyPermission(LoginUser loginUser, Collection<String> permissions) {
        PermissionCheckReqDTO reqDTO = new PermissionCheckReqDTO();
        reqDTO.setUserId(loginUser.userId());
        reqDTO.setUserType(loginUser.userType());
        reqDTO.setPermissions(permissions);
        return Boolean.TRUE.equals(infraPermissionClient.hasAnyPermission(reqDTO).requireData());
    }

    /**
     * 熔断/限流降级：按无权限处理
     *
     * @param loginUser   当前登录用户
     * @param permissions 接口要求的权限标识
     * @param ex          Sentinel 阻塞异常
     * @return 恒为 false
     */
    public boolean hasAnyPermissionBlocked(LoginUser loginUser, Collection<String> permissions, BlockException ex) {
        log.error("[hasAnyPermissionBlocked][权限校验被熔断或限流，按无权限处理] userId={}, rule={}",
                loginUser.userId(), ex.getRule());
        return false;
    }

    /**
     * 异常降级：按无权限处理
     *
     * @param loginUser   当前登录用户
     * @param permissions 接口要求的权限标识
     * @param ex          调用异常
     * @return 恒为 false
     */
    public boolean hasAnyPermissionFailed(LoginUser loginUser, Collection<String> permissions, Throwable ex) {
        log.error("[hasAnyPermissionFailed][权限校验失败，按无权限处理] userId={}", loginUser.userId(), ex);
        return false;
    }
}
