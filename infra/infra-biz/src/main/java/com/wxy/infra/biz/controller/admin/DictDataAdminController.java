package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.service.InfraDictDataService;
import com.wxy.infra.biz.vo.admin.DictDataCreateReqVO;
import com.wxy.infra.biz.vo.admin.DictDataPageReqVO;
import com.wxy.infra.biz.vo.admin.DictDataRespVO;
import com.wxy.infra.biz.vo.admin.DictDataSimpleRespVO;
import com.wxy.infra.biz.vo.admin.DictDataUpdateReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台字典数据接口：最终对外路径为 {@code /admin-api/dict-data/...}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "管理后台 - 字典数据")
@RestController
@RequestMapping("/dict-data")
public class DictDataAdminController {

    /** 字典数据服务 */
    @Resource
    private InfraDictDataService infraDictDataService;

    /**
     * 新增字典数据
     *
     * @param reqVO 新增入参
     * @return 新字典数据 ID
     */
    @Operation(summary = "新增字典数据", description = "类型必须存在，同一类型下字典值唯一")
    @RequiresPermission(InfraPermissionConstant.DICT_DATA_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody DictDataCreateReqVO reqVO) {
        return Result.success(infraDictDataService.createDictData(reqVO));
    }

    /**
     * 修改字典数据
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改字典数据", description = "可改所属类型；会校验新类型下字典值不重复")
    @RequiresPermission(InfraPermissionConstant.DICT_DATA_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody DictDataUpdateReqVO reqVO) {
        infraDictDataService.updateDictData(reqVO);
        return Result.success();
    }

    /**
     * 删除字典数据
     *
     * @param id 字典数据 ID
     * @return 空响应
     */
    @Operation(summary = "删除字典数据", description = "逻辑删除")
    @RequiresPermission(InfraPermissionConstant.DICT_DATA_DELETE)
    @PostMapping("/delete")
    public Result<Void> delete(@Parameter(description = "字典数据 ID") @RequestParam Long id) {
        infraDictDataService.deleteDictData(id);
        return Result.success();
    }

    /**
     * 查询字典数据详情
     *
     * @param id 字典数据 ID
     * @return 字典数据详情
     */
    @Operation(summary = "查询字典数据详情")
    @RequiresPermission(InfraPermissionConstant.DICT_DATA_QUERY)
    @GetMapping("/getById")
    public Result<DictDataRespVO> getById(@Parameter(description = "字典数据 ID") @RequestParam Long id) {
        return Result.success(infraDictDataService.getDictData(id));
    }

    /**
     * 分页查询字典数据
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询字典数据", description = "按类型编码精确匹配，标签模糊匹配")
    @RequiresPermission(InfraPermissionConstant.DICT_DATA_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<DictDataRespVO>> page(@Validated @RequestBody DictDataPageReqVO reqVO) {
        return Result.success(infraDictDataService.pageDictData(reqVO));
    }

    /**
     * 按类型编码查询启用的字典数据
     *
     * @param dictType 字典类型编码
     * @return 字典数据精简列表
     */
    @Operation(summary = "按类型查询字典数据", description = "只返回启用数据并按排序号升序，前端下拉/标签展示用")
    @RequiresPermission(InfraPermissionConstant.DICT_DATA_QUERY)
    @GetMapping("/listByType")
    public Result<List<DictDataSimpleRespVO>> listByType(
            @Parameter(description = "字典类型编码") @RequestParam String dictType) {
        return Result.success(infraDictDataService.listDictDataByType(dictType));
    }
}
