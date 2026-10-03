package com.wxy.infra.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.constant.InfraApiConstant;
import com.wxy.infra.api.dto.PermissionCheckReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * infra 对外发布的权限服务接口：其他服务没有角色菜单数据时，调它判断用户权限。
 *
 * <p>权限数据只有 infra 有（用户 → 角色 → 菜单），所以这里只提供「有没有权限」的判断，
 * 不返回权限清单，避免各服务自己拼一套判断逻辑。
 *
 * @author wxy
 * @date 2026/10/03
 */
@FeignClient(name = InfraApiConstant.SERVICE_NAME, contextId = "infraPermissionClient")
public interface InfraPermissionClient {

    /**
     * 判断用户是否拥有其中任意一个权限
     *
     * @param reqDTO 校验入参
     * @return true 表示拥有任意一个权限
     */
    @PostMapping(InfraApiConstant.PERMISSION_API_PREFIX + InfraApiConstant.PERMISSION_HAS_ANY_PATH)
    Result<Boolean> hasAnyPermission(@RequestBody PermissionCheckReqDTO reqDTO);
}
