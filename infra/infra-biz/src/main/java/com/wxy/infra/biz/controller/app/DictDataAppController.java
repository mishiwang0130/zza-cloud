package com.wxy.infra.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraDictDataService;
import com.wxy.infra.biz.vo.app.DictDataAppRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端字典数据接口：最终对外路径为 {@code /api/infra/app-api/dict-data/...}。
 *
 * <p>整个类标 {@link PermitAll}：访客端匿名浏览房源时要拿朝向、标签、配套等字典渲染中文名，
 * 此时还没有登录态；字典是配置数据，只读且不含隐私。
 *
 * @author wxy
 * @date 2026/10/04
 */
@PermitAll
@Tag(name = "用户端 - 字典数据")
@RestController
@RequestMapping("/dict-data")
public class DictDataAppController {

    /** 字典数据服务 */
    @Resource
    private InfraDictDataService infraDictDataService;

    /**
     * 按类型编码查询启用的字典数据
     *
     * @param type 字典类型编码
     * @return 字典数据列表（只含标签与值）
     */
    @Operation(summary = "按类型查询字典数据", description = "只返回启用数据并按排序号升序，供访客端下拉 / 标签展示")
    @GetMapping("/listByType")
    public Result<List<DictDataAppRespVO>> listByType(
            @Parameter(description = "字典类型编码") @RequestParam("type") String type) {
        return Result.success(infraDictDataService.listAppDictDataByType(type));
    }
}
