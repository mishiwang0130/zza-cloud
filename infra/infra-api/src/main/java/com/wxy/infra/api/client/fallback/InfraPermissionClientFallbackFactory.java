package com.wxy.infra.api.client.fallback;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraPermissionClient;
import com.wxy.infra.api.dto.PermissionCheckReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 权限服务客户端的降级工厂：远程调用失败或被 Sentinel 熔断时触发。
 *
 * <p><b>降级一律按无权限处理</b>：权限校验同样不能因为依赖不可用而放行。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
@Component
public class InfraPermissionClientFallbackFactory implements FallbackFactory<InfraPermissionClient> {

    /**
     * 创建降级实现
     *
     * @param cause 触发降级的异常（调用失败或熔断）
     * @return 降级的客户端实现
     */
    @Override
    public InfraPermissionClient create(Throwable cause) {
        return new InfraPermissionClient() {

            /**
             * 降级处理：记录原因并按无权限处理
             *
             * @param reqDTO 校验入参
             * @return 无权限结果
             */
            @Override
            public Result<Boolean> hasAnyPermission(PermissionCheckReqDTO reqDTO) {
                log.error("[hasAnyPermission][调用 infra 校验权限失败，按无权限处理] cause={}",
                        cause.getMessage(), cause);
                return Result.success(false);
            }
        };
    }
}
