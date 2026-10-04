package com.wxy.infra.api.client.fallback;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraAreaClient;
import com.wxy.infra.api.dto.AreaDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 行政区划客户端的降级工厂：远程调用失败或被 Sentinel 熔断时触发。
 *
 * <p><b>降级一律失败</b>：区划拿不到时，按 ID 查名字会得到 null、按市展开会得到空列表，
 * 两种情况都会把错误数据当成「查不到」返回给前端，不如直接把失败暴露出来。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Component
public class InfraAreaClientFallbackFactory implements FallbackFactory<InfraAreaClient> {

    /**
     * 创建降级实现
     *
     * @param cause 触发降级的异常（调用失败或熔断）
     * @return 降级的客户端实现
     */
    @Override
    public InfraAreaClient create(Throwable cause) {
        return new InfraAreaClient() {

            /**
             * 降级处理：记录原因并返回失败响应
             *
             * @param parentId 上级区划 ID
             * @return 失败的统一响应
             */
            @Override
            public Result<List<AreaDTO>> listChildren(Long parentId) {
                log.error("[listChildren][调用 infra 查询子级区划失败] parentId={}, cause={}",
                        parentId, cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "行政区划服务暂时不可用，请稍后重试");
            }

            /**
             * 降级处理：记录原因并返回失败响应
             *
             * @return 失败的统一响应
             */
            @Override
            public Result<List<AreaDTO>> listTree() {
                log.error("[listTree][调用 infra 查询行政区划树失败] cause={}", cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "行政区划服务暂时不可用，请稍后重试");
            }
        };
    }
}
