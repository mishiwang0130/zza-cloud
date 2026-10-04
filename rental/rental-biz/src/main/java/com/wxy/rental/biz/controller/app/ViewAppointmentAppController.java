package com.wxy.rental.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.service.RentalAppAppointmentService;
import com.wxy.rental.biz.vo.app.AppViewAppointmentCancelReqVO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentCreateReqVO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端看房预约接口：最终对外路径为 {@code /api/rental/app-api/view-appointment/...}。
 *
 * <p>三个接口都要求登录（没有 {@code @PermitAll}）：预约人与被查/被取消的预约都取自登录上下文，前端不传 userId。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "用户端 - 看房预约")
@RestController
@RequestMapping("/view-appointment")
public class ViewAppointmentAppController {

    /** 用户端看房预约服务 */
    @Resource
    private RentalAppAppointmentService rentalAppAppointmentService;

    /**
     * 提交看房预约
     *
     * @param reqVO 预约入参
     * @return 新预约 ID
     */
    @Operation(summary = "提交看房预约", description = "只能预约已发布公寓；预约人取当前登录用户，只记用户 ID，不存姓名与手机号")
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody AppViewAppointmentCreateReqVO reqVO) {
        return Result.success(rentalAppAppointmentService.createAppointment(reqVO));
    }

    /**
     * 分页查询我的看房预约
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    @Operation(summary = "分页查询我的看房预约", description = "只查自己；列表已带齐展示字段，没有单独的详情接口")
    @PostMapping("/page")
    public Result<PageRespVO<AppViewAppointmentRespVO>> page(@Validated @RequestBody PageReqVO reqVO) {
        return Result.success(rentalAppAppointmentService.pageAppointment(reqVO));
    }

    /**
     * 取消我的看房预约
     *
     * @param reqVO 取消入参
     * @return 空响应
     */
    @Operation(summary = "取消看房预约", description = "只能取消自己的、状态为待看房的预约")
    @PostMapping("/cancel")
    public Result<Void> cancel(@Validated @RequestBody AppViewAppointmentCancelReqVO reqVO) {
        rentalAppAppointmentService.cancelAppointment(reqVO.getId());
        return Result.success();
    }
}
