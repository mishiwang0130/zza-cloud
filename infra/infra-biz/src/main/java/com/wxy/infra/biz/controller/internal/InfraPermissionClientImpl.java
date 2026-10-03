package com.wxy.infra.biz.controller.internal;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraPermissionClient;
import com.wxy.infra.api.dto.PermissionCheckReqDTO;
import com.wxy.infra.biz.service.InfraPermissionService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Set;
import org.springframework.web.bind.annotation.RestController;

/**
 * infra 权限服务间接口的实现：实现 infra-api 发布的 {@link InfraPermissionClient}。
 *
 * <p>规则与 infra 自身的接口权限校验保持一致（超管放行、非 admin 端拒绝、其余按
 * 用户 → 角色 → 菜单 算出的权限集合匹配），所以这里复用 {@link InfraPermissionService}，
 * 不另写一套判断。
 *
 * <p>与凭证接口同理：路径与参数绑定都在 client 里，本类只写实现；
 * 服务间接口不对外，因此不进接口文档（{@link Hidden}）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Hidden
@RestController
public class InfraPermissionClientImpl implements InfraPermissionClient {

    /** 权限服务 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /**
     * 判断用户是否拥有其中任意一个权限
     *
     * @param reqDTO 校验入参
     * @return true 表示拥有任意一个权限
     */
    @Override
    public Result<Boolean> hasAnyPermission(PermissionCheckReqDTO reqDTO) {
        LoginUser loginUser = new LoginUser(reqDTO.getUserId(), reqDTO.getUserType(), null);
        return Result.success(hasAnyPermission(loginUser, reqDTO.getPermissions()));
    }

    /**
     * 按 infra 的权限规则判断：超管放行、非 admin 端拒绝、其余比对权限集合
     *
     * @param loginUser   调用方透传的身份
     * @param permissions 需要判断的权限标识
     * @return 拥有任意一个权限时返回 true
     */
    private boolean hasAnyPermission(LoginUser loginUser, Collection<String> permissions) {
        if (infraPermissionService.isSuperAdmin(loginUser.userId())) {
            return true;
        }
        // 管理后台的菜单权限不适用于其他端，app 端权限体系接入前按无权限处理
        if (!UserTypeEnum.ADMIN.getValue().equals(loginUser.userType())) {
            return false;
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
