package com.wxy.rental.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.rental.biz.constant.RentalPermissionConstant;
import com.wxy.rental.biz.service.RentalFeeItemService;
import com.wxy.rental.biz.vo.admin.FeeItemCreateReqVO;
import com.wxy.rental.biz.vo.admin.FeeItemDeleteReqVO;
import com.wxy.rental.biz.vo.admin.FeeItemRespVO;
import com.wxy.rental.biz.vo.admin.FeeItemUpdateReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台费用项接口：方法上的权限标识与 {@code infra_menu.perms} 里的按钮权限一一对应。
 *
 * <p>最终对外路径为 {@code /api/rental/admin-api/fee-item/...}；端前缀由 common-webmvc
 * 按包名自动拼接，这里只写业务路径。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "管理后台 - 费用项")
@RestController
@RequestMapping("/fee-item")
public class FeeItemAdminController {

    /** 费用项服务 */
    @Resource
    private RentalFeeItemService rentalFeeItemService;

    /**
     * 新增费用项
     *
     * @param reqVO 新增入参
     * @return 新费用项 ID
     */
    @Operation(summary = "新增费用项", description = "名称唯一")
    @RequiresPermission(RentalPermissionConstant.FEE_ITEM_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody FeeItemCreateReqVO reqVO) {
        return Result.success(rentalFeeItemService.createFeeItem(reqVO));
    }

    /**
     * 修改费用项
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改费用项", description = "名称唯一；改动会同时影响引用它的公寓")
    @RequiresPermission(RentalPermissionConstant.FEE_ITEM_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody FeeItemUpdateReqVO reqVO) {
        rentalFeeItemService.updateFeeItem(reqVO);
        return Result.success();
    }

    /**
     * 删除费用项
     *
     * @param reqVO 删除入参
     * @return 空响应
     */
    @Operation(summary = "删除费用项", description = "逻辑删除；已被公寓引用时不允许删除")
    @RequiresPermission(RentalPermissionConstant.FEE_ITEM_DELETE)
    @PostMapping("/delete")
    public Result<Void> delete(@Validated @RequestBody FeeItemDeleteReqVO reqVO) {
        rentalFeeItemService.deleteFeeItem(reqVO.getId());
        return Result.success();
    }

    /**
     * 查询全部费用项
     *
     * @return 费用项列表
     */
    @Operation(summary = "查询费用项列表", description = "小配置表不分页；编辑弹窗直接用列表行数据")
    @RequiresPermission(RentalPermissionConstant.FEE_ITEM_QUERY)
    @GetMapping("/list")
    public Result<List<FeeItemRespVO>> list() {
        return Result.success(rentalFeeItemService.listFeeItem());
    }
}
