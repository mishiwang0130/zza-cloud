package com.wxy.rental.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.constant.RentalPermissionConstant;
import com.wxy.rental.biz.service.RentalViewAppointmentService;
import com.wxy.rental.biz.vo.admin.ViewAppointmentPageReqVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentRespVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentUpdateStatusReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台看房预约接口：最终对外路径为 {@code /api/rental/admin-api/view-appointment/...}。
 *
 * <p>只有列表与状态流转两个接口：预约由 App 端发起，列表返回的就是列表与详情弹窗要的全部字段，所以不出详情接口。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "管理后台 - 看房预约")
@RestController
@RequestMapping("/view-appointment")
public class ViewAppointmentAdminController {

    /** 看房预约服务 */
    @Resource
    private RentalViewAppointmentService rentalViewAppointmentService;

    /**
     * 分页查询看房预约
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询看房预约", description = "列表与详情弹窗共用同一份字段；租客昵称待 infra 提供 App 用户批量查询后回填")
    @RequiresPermission(RentalPermissionConstant.VIEW_APPOINTMENT_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<ViewAppointmentRespVO>> page(@Validated @RequestBody ViewAppointmentPageReqVO reqVO) {
        return Result.success(rentalViewAppointmentService.pageAppointment(reqVO));
    }

    /**
     * 看房预约状态流转
     *
     * @param reqVO 状态流转入参
     * @return 空响应
     */
    @Operation(summary = "看房预约状态流转", description = "只允许待看房 → 已看房 / 已取消")
    @RequiresPermission(RentalPermissionConstant.VIEW_APPOINTMENT_UPDATE_STATUS)
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@Validated @RequestBody ViewAppointmentUpdateStatusReqVO reqVO) {
        rentalViewAppointmentService.updateStatus(reqVO);
        return Result.success();
    }
}
