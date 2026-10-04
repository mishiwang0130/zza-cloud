package com.wxy.infra.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.fallback.InfraFileClientFallbackFactory;
import com.wxy.infra.api.constant.InfraApiConstant;
import com.wxy.infra.api.dto.FileRespDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * infra 对外发布的文件读取接口：其他服务只存了 {@code fileId} 时，调它换回预签名访问地址。
 *
 * <p>只做「按 ID 批量查」，不提供列表与删除：文件归属由业务服务自己维护（例如 rental 的
 * {@code rental_image.file_id}），infra 只负责元数据与地址签发。
 *
 * <p><b>熔断降级</b>：调用失败或被熔断时返回失败响应，调用方 {@code requireData()} 抛异常。
 * 生效需要 {@code feign.sentinel.enabled: true}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@FeignClient(name = InfraApiConstant.SERVICE_NAME, contextId = "infraFileClient",
        fallbackFactory = InfraFileClientFallbackFactory.class)
public interface InfraFileClient {

    /**
     * 按 ID 批量查询文件（含预签名访问地址）
     *
     * @param ids 文件 ID 列表，为空时返回空列表
     * @return 文件列表，查不到的 ID 直接不返回
     */
    @GetMapping("/internal-api/file/listByIds")
    Result<List<FileRespDTO>> listByIds(@RequestParam("ids") List<Long> ids);
}
