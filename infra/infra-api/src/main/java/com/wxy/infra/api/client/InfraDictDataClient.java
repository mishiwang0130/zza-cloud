package com.wxy.infra.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.fallback.InfraDictDataClientFallbackFactory;
import com.wxy.infra.api.constant.InfraApiConstant;
import com.wxy.infra.api.dto.DictDataSimpleDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * infra 对外发布的字典读取接口：其他服务没有字典表时，调它拿某个类型下的「标签 + 值」。
 *
 * <p>为什么单独开一个服务间接口，而不是让调用方走后端管理接口 {@code /admin-api/dict-data/listByType}：
 * 那个接口挂在 {@code /admin-api} 下、要过管理端权限，服务间调用既没有管理端身份也不该依赖它；
 * 这里走 {@code /internal-api}，由网关挡掉外部访问，只允许内网 OpenFeign 直连。
 *
 * <p><b>熔断降级</b>：{@link #fallbackFactory} 在调用失败或被熔断时返回失败响应，
 * 调用方 {@code requireData()} 会直接抛异常——字典拿不到时宁可报错，也不要静默返回空标签。
 * 生效需要 {@code feign.sentinel.enabled: true}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@FeignClient(name = InfraApiConstant.SERVICE_NAME, contextId = "infraDictDataClient",
        fallbackFactory = InfraDictDataClientFallbackFactory.class)
public interface InfraDictDataClient {

    /**
     * 按字典类型编码查询启用的字典数据（按排序号升序）
     *
     * @param dictType 字典类型编码
     * @return 字典数据精简列表
     */
    @GetMapping("/internal-api/dict-data/listByType")
    Result<List<DictDataSimpleDTO>> listByType(@RequestParam("dictType") String dictType);
}
