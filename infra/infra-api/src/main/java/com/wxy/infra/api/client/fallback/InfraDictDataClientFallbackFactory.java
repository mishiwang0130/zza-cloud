package com.wxy.infra.api.client.fallback;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraDictDataClient;
import com.wxy.infra.api.dto.DictDataSimpleDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 字典读取客户端的降级工厂：远程调用失败或被 Sentinel 熔断时触发。
 *
 * <p><b>降级一律失败</b>：不返回空列表。空列表与「这个类型下确实没有字典数据」无法区分，
 * 调用方会把有数据的标签静默展示成空，排查时只能一路往上翻日志。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Component
public class InfraDictDataClientFallbackFactory implements FallbackFactory<InfraDictDataClient> {

    /**
     * 创建降级实现
     *
     * @param cause 触发降级的异常（调用失败或熔断）
     * @return 降级的客户端实现
     */
    @Override
    public InfraDictDataClient create(Throwable cause) {
        return new InfraDictDataClient() {

            /**
             * 降级处理：记录原因并返回失败响应
             *
             * @param dictType 字典类型编码
             * @return 失败的统一响应
             */
            @Override
            public Result<List<DictDataSimpleDTO>> listByType(String dictType) {
                log.error("[listByType][调用 infra 查询字典失败] dictType={}, cause={}",
                        dictType, cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "字典服务暂时不可用，请稍后重试");
            }
        };
    }
}
