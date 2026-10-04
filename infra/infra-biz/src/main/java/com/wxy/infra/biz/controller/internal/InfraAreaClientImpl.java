package com.wxy.infra.biz.controller.internal;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraAreaClient;
import com.wxy.infra.api.dto.AreaDTO;
import com.wxy.infra.biz.convert.InfraAreaConvert;
import com.wxy.infra.biz.service.InfraAreaService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
 * infra 行政区划服务间接口的实现：实现 infra-api 发布的 {@link InfraAreaClient}。
 *
 * <p>与管理端 {@code AreaAdminController} 共用 {@link InfraAreaService}，只多一步「返回体 → DTO」转换。
 *
 * <p>路径与参数绑定都在 client 上，本类只写实现；服务间接口不对外，因此不进接口文档（{@link Hidden}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Hidden
@RestController
public class InfraAreaClientImpl implements InfraAreaClient {

    /** 行政区划服务 */
    @Resource
    private InfraAreaService infraAreaService;

    /** 行政区划转换器 */
    @Resource
    private InfraAreaConvert infraAreaConvert;

    /**
     * 查询某一级下的子级区划
     *
     * @param parentId 上级区划 ID，不传表示取省级
     * @return 子级区划列表
     */
    @Override
    public Result<List<AreaDTO>> listChildren(Long parentId) {
        return Result.success(infraAreaConvert.toDTOList(infraAreaService.listChildren(parentId)));
    }

    /**
     * 查询完整的省市区树
     *
     * @return 省级为根的树
     */
    @Override
    public Result<List<AreaDTO>> listTree() {
        return Result.success(infraAreaConvert.toDTOList(infraAreaService.listTree()));
    }
}
