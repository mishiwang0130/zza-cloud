package com.wxy.rental.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.service.RentalAppLeaseService;
import com.wxy.rental.biz.vo.app.AppLeaseActionReqVO;
import com.wxy.rental.biz.vo.app.AppLeaseItemRespVO;
import com.wxy.rental.biz.vo.app.AppLeaseRespVO;
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
 * 用户端租约接口：最终对外路径为 {@code /api/rental/app-api/lease/...}。
 *
 * <p>五个接口都要求登录，且只允许操作自己的租约：别人的租约一律按「租约不存在」返回，不暴露存在性。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "用户端 - 租约")
@RestController
@RequestMapping("/lease")
public class LeaseAppController {

    /** 用户端租约服务 */
    @Resource
    private RentalAppLeaseService rentalAppLeaseService;

    /**
     * 分页查询我的租约
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    @Operation(summary = "分页查询我的租约", description = "只查自己；卡片带房间封面图与租约状态")
    @PostMapping("/page")
    public Result<PageRespVO<AppLeaseItemRespVO>> page(@Validated @RequestBody PageReqVO reqVO) {
        return Result.success(rentalAppLeaseService.pageLease(reqVO));
    }

    /**
     * 查询我的租约详情
     *
     * @param id 租约 ID
     * @return 租约详情
     */
    @Operation(summary = "查询我的租约详情", description = "含合同文件的预签名地址与房间图片")
    @GetMapping("/getById")
    public Result<AppLeaseRespVO> getById(@Parameter(description = "租约 ID") @RequestParam Long id) {
        return Result.success(rentalAppLeaseService.getLease(id));
    }

    /**
     * 确认签约
     *
     * @param reqVO 动作入参
     * @return 空响应
     */
    @Operation(summary = "确认签约", description = "1 签约待确认 → 2 已签约")
    @PostMapping("/confirm")
    public Result<Void> confirm(@Validated @RequestBody AppLeaseActionReqVO reqVO) {
        rentalAppLeaseService.confirm(reqVO.getId());
        return Result.success();
    }

    /**
     * 申请退租
     *
     * @param reqVO 动作入参
     * @return 空响应
     */
    @Operation(summary = "申请退租", description = "2 已签约 → 5 退租待确认，最终是否退租由运营确认")
    @PostMapping("/apply-withdraw")
    public Result<Void> applyWithdraw(@Validated @RequestBody AppLeaseActionReqVO reqVO) {
        rentalAppLeaseService.applyWithdraw(reqVO.getId());
        return Result.success();
    }

    /**
     * 申请续约
     *
     * @param reqVO 动作入参
     * @return 空响应
     */
    @Operation(summary = "申请续约", description = "2 已签约 → 7 续约待确认，最终续约条款由运营确认")
    @PostMapping("/apply-renew")
    public Result<Void> applyRenew(@Validated @RequestBody AppLeaseActionReqVO reqVO) {
        rentalAppLeaseService.applyRenew(reqVO.getId());
        return Result.success();
    }
}
