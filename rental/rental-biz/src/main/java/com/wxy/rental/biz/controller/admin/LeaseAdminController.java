package com.wxy.rental.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.constant.RentalPermissionConstant;
import com.wxy.rental.biz.service.RentalLeaseService;
import com.wxy.rental.biz.vo.admin.LeaseCreateReqVO;
import com.wxy.rental.biz.vo.admin.LeasePageItemRespVO;
import com.wxy.rental.biz.vo.admin.LeasePageReqVO;
import com.wxy.rental.biz.vo.admin.LeaseRespVO;
import com.wxy.rental.biz.vo.admin.LeaseUpdateReqVO;
import com.wxy.rental.biz.vo.admin.LeaseUpdateStatusReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台租约接口：路径遵循「资源 + 动作」，最终对外路径为 {@code /api/rental/admin-api/lease/...}。
 *
 * <p>没有删除接口：要作废就把状态置为 3 已取消。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "管理后台 - 租约")
@RestController
@RequestMapping("/lease")
public class LeaseAdminController {

    /** 租约服务 */
    @Resource
    private RentalLeaseService rentalLeaseService;

    /**
     * 新增租约
     *
     * @param reqVO 新增入参
     * @return 新租约 ID
     */
    @Operation(summary = "新增租约", description = "房间必须存在且属于提交的公寓；房间不能已有生效中的租约；押金不传按租金 × 押金月数计算")
    @RequiresPermission(RentalPermissionConstant.LEASE_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody LeaseCreateReqVO reqVO) {
        return Result.success(rentalLeaseService.createLease(reqVO));
    }

    /**
     * 修改租约
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改租约", description = "只允许改条款；已取消 / 已到期 / 已退租的租约只能改合同文件与备注")
    @RequiresPermission(RentalPermissionConstant.LEASE_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody LeaseUpdateReqVO reqVO) {
        rentalLeaseService.updateLease(reqVO);
        return Result.success();
    }

    /**
     * 查询租约详情
     *
     * @param id 租约 ID
     * @return 租约详情
     */
    @Operation(summary = "查询租约详情", description = "含公寓名与房间号；不含租客实名信息")
    @RequiresPermission(RentalPermissionConstant.LEASE_QUERY)
    @GetMapping("/getById")
    public Result<LeaseRespVO> getById(@Parameter(description = "租约 ID") @RequestParam Long id) {
        return Result.success(rentalLeaseService.getLease(id));
    }

    /**
     * 分页查询租约列表
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询租约列表", description = "只返回列表字段；租客昵称与手机待 infra 提供 App 用户批量查询后回填")
    @RequiresPermission(RentalPermissionConstant.LEASE_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<LeasePageItemRespVO>> page(@Validated @RequestBody LeasePageReqVO reqVO) {
        return Result.success(rentalLeaseService.pageLease(reqVO));
    }

    /**
     * 租约状态流转
     *
     * @param reqVO 状态流转入参
     * @return 空响应
     */
    @Operation(summary = "租约状态流转", description = "严格按状态流转表执行，非法迁移一律拒绝")
    @RequiresPermission(RentalPermissionConstant.LEASE_UPDATE_STATUS)
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@Validated @RequestBody LeaseUpdateStatusReqVO reqVO) {
        rentalLeaseService.updateStatus(reqVO);
        return Result.success();
    }
}
