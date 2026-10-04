package com.wxy.infra.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.fallback.InfraAreaClientFallbackFactory;
import com.wxy.infra.api.constant.InfraApiConstant;
import com.wxy.infra.api.dto.AreaDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * infra 对外发布的行政区划读取接口：供其他服务把区县 ID 换成名称、把市展开成区县列表。
 *
 * <p>区划是标准数据，只读不写；调用方按需要选子级查询或整棵树，两种都提供是为了让调用方
 * 能自己决定「一次拉全缓存起来」还是「按层懒加载」，而不是被迫接受 infra 的选择。
 *
 * <p><b>熔断降级</b>：调用失败或被熔断时返回失败响应，调用方 {@code requireData()} 抛异常。
 * 生效需要 {@code feign.sentinel.enabled: true}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@FeignClient(name = InfraApiConstant.SERVICE_NAME, contextId = "infraAreaClient",
        fallbackFactory = InfraAreaClientFallbackFactory.class)
public interface InfraAreaClient {

    /**
     * 查询某一级下的子级区划
     *
     * @param parentId 上级区划 ID，不传（null）表示取省级
     * @return 子级区划列表
     */
    @GetMapping("/internal-api/area/listChildren")
    Result<List<AreaDTO>> listChildren(@RequestParam(value = "parentId", required = false) Long parentId);

    /**
     * 查询完整的省市区树
     *
     * @return 省级为根的树
     */
    @GetMapping("/internal-api/area/listTree")
    Result<List<AreaDTO>> listTree();
}
