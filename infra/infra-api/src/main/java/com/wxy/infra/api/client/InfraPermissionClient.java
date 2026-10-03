package com.wxy.infra.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.fallback.InfraPermissionClientFallbackFactory;
import com.wxy.infra.api.dto.PermissionCheckReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * infra 对外发布的权限服务接口：其他服务没有角色菜单数据时，调它判断用户权限。
 *
 * <p>权限数据只有 infra 有（用户 → 角色 → 菜单），所以这里只提供「有没有权限」的判断，
 * 不返回权限清单，避免各服务自己拼一套判断逻辑。
 *
 * <p><b>熔断降级</b>：{@link #fallbackFactory} 在远程调用失败或被熔断时返回「无权限」（fail-closed）。
 * 生效同样依赖 {@code feign.sentinel.enabled: true}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@FeignClient(name = "infra", contextId = "infraPermissionClient",
        fallbackFactory = InfraPermissionClientFallbackFactory.class)
public interface InfraPermissionClient {

    /**
     * 判断用户是否拥有其中任意一个权限
     *
     * @param reqDTO 校验入参
     * @return true 表示拥有任意一个权限
     */
    @PostMapping("/internal-api/permission/has-any")
    Result<Boolean> hasAnyPermission(@Validated @RequestBody PermissionCheckReqDTO reqDTO);
}
