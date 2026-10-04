package com.wxy.infra.biz.controller.internal;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraDictDataClient;
import com.wxy.infra.api.dto.DictDataSimpleDTO;
import com.wxy.infra.biz.convert.InfraDictDataConvert;
import com.wxy.infra.biz.service.InfraDictDataService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
 * infra 字典读取服务间接口的实现：实现 infra-api 发布的 {@link InfraDictDataClient}。
 *
 * <p>直接复用管理端在用的 {@link InfraDictDataService}，只多做一步「返回体 → DTO」的转换，
 * 不另写一套查询逻辑——两边口径必须一致，否则管理端能看到的字典与别的服务拿到的字典会对不上。
 *
 * <p>路径与参数绑定都在 client 上，本类只写实现；服务间接口不对外，因此不进接口文档（{@link Hidden}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Hidden
@RestController
public class InfraDictDataClientImpl implements InfraDictDataClient {

    /** 字典数据服务 */
    @Resource
    private InfraDictDataService infraDictDataService;

    /** 字典数据转换器 */
    @Resource
    private InfraDictDataConvert infraDictDataConvert;

    /**
     * 按字典类型编码查询启用的字典数据
     *
     * @param dictType 字典类型编码
     * @return 字典数据精简列表
     */
    @Override
    public Result<List<DictDataSimpleDTO>> listByType(String dictType) {
        return Result.success(infraDictDataConvert.toSimpleDTOList(
                infraDictDataService.listDictDataByType(dictType)));
    }
}
