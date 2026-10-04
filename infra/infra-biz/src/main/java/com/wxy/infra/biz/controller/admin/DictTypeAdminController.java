package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.service.InfraDictTypeService;
import com.wxy.infra.biz.vo.admin.DictTypeCreateReqVO;
import com.wxy.infra.biz.vo.admin.DictTypePageReqVO;
import com.wxy.infra.biz.vo.admin.DictTypeRespVO;
import com.wxy.infra.biz.vo.admin.DictTypeSimpleRespVO;
import com.wxy.infra.biz.vo.admin.DictTypeUpdateReqVO;
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
 * 管理后台字典类型接口：最终对外路径为 {@code /admin-api/dict-type/...}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "管理后台 - 字典类型")
@RestController
@RequestMapping("/dict-type")
public class DictTypeAdminController {

    /** 字典类型服务 */
    @Resource
    private InfraDictTypeService infraDictTypeService;

    /**
     * 新增字典类型
     *
     * @param reqVO 新增入参
     * @return 新字典类型 ID
     */
    @Operation(summary = "新增字典类型", description = "编码唯一，只能由字母、数字、下划线、中划线组成且以字母开头")
    @RequiresPermission(InfraPermissionConstant.DICT_TYPE_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody DictTypeCreateReqVO reqVO) {
        return Result.success(infraDictTypeService.createDictType(reqVO));
    }

    /**
     * 修改字典类型
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改字典类型", description = "改编码时会同步刷新该类型下的字典数据")
    @RequiresPermission(InfraPermissionConstant.DICT_TYPE_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody DictTypeUpdateReqVO reqVO) {
        infraDictTypeService.updateDictType(reqVO);
        return Result.success();
    }

    /**
     * 删除字典类型
     *
     * @param id 字典类型 ID
     * @return 空响应
     */
    @Operation(summary = "删除字典类型", description = "类型下还有字典数据时不允许删除")
    @RequiresPermission(InfraPermissionConstant.DICT_TYPE_DELETE)
    @PostMapping("/delete")
    public Result<Void> delete(@Parameter(description = "字典类型 ID") @RequestParam Long id) {
        infraDictTypeService.deleteDictType(id);
        return Result.success();
    }

    /**
     * 查询字典类型详情
     *
     * @param id 字典类型 ID
     * @return 字典类型详情
     */
    @Operation(summary = "查询字典类型详情")
    @RequiresPermission(InfraPermissionConstant.DICT_TYPE_QUERY)
    @GetMapping("/getById")
    public Result<DictTypeRespVO> getById(@Parameter(description = "字典类型 ID") @RequestParam Long id) {
        return Result.success(infraDictTypeService.getDictType(id));
    }

    /**
     * 分页查询字典类型
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询字典类型", description = "支持名称、编码与状态过滤")
    @RequiresPermission(InfraPermissionConstant.DICT_TYPE_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<DictTypeRespVO>> page(@Validated @RequestBody DictTypePageReqVO reqVO) {
        return Result.success(infraDictTypeService.pageDictType(reqVO));
    }

    /**
     * 查询启用字典类型列表
     *
     * @return 字典类型精简列表
     */
    @Operation(summary = "查询字典类型列表", description = "只返回启用类型，用于字典数据的类型下拉")
    @RequiresPermission(InfraPermissionConstant.DICT_TYPE_QUERY)
    @GetMapping("/list")
    public Result<List<DictTypeSimpleRespVO>> list() {
        return Result.success(infraDictTypeService.listDictType());
    }
}
