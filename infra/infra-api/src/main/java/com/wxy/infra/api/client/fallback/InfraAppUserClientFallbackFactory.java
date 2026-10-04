package com.wxy.infra.api.client.fallback;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraAppUserClient;
import com.wxy.infra.api.dto.AppUserSimpleDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户端用户读取客户端的降级工厂：远程调用失败或被 Sentinel 熔断时触发。
 *
 * <p><b>降级一律失败</b>：返回空列表会让调用方以为「这些用户都不存在」，
 * 于是后台列表里的预约人姓名与手机号整列变空却没有任何报错，属于最难排查的一类问题。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class InfraAppUserClientFallbackFactory implements FallbackFactory<InfraAppUserClient> {

    /**
     * 创建降级实现
     *
     * @param cause 触发降级的异常（调用失败或熔断）
     * @return 降级的客户端实现
     */
    @Override
    public InfraAppUserClient create(Throwable cause) {
        return new InfraAppUserClient() {

            /**
             * 降级处理：记录原因并返回失败响应
             *
             * @param ids 用户 ID 列表
             * @return 失败的统一响应
             */
            @Override
            public Result<List<AppUserSimpleDTO>> listByIds(List<Long> ids) {
                log.error("[listByIds][调用 infra 查询用户端用户失败] ids={}, cause={}", ids, cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "用户服务暂时不可用，请稍后重试");
            }
        };
    }
}
