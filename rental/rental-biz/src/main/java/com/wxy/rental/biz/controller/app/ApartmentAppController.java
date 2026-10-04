package com.wxy.rental.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.service.RentalAppApartmentService;
import com.wxy.rental.biz.vo.app.AppApartmentItemRespVO;
import com.wxy.rental.biz.vo.app.AppApartmentPageReqVO;
import com.wxy.rental.biz.vo.app.AppApartmentRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端公寓接口：最终对外路径为 {@code /api/rental/app-api/apartment/...}。
 *
 * <p>两个接口都标 {@link PermitAll}：房源浏览不需要登录，未登录用户也能看列表与详情；确实需要身份的动作（预约、浏览记录）在各自的接口里校验登录。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "用户端 - 公寓")
@RestController
@RequestMapping("/apartment")
public class ApartmentAppController {

    /** 用户端公寓服务 */
    @Resource
    private RentalAppApartmentService rentalAppApartmentService;

    /**
     * 分页查询已发布公寓
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    @PermitAll
    @Operation(summary = "分页查询公寓列表", description = "只返回已发布公寓；租金 / 面积 / 室数条件按公寓下的已发布房间过滤")
    @PostMapping("/page")
    public Result<PageRespVO<AppApartmentItemRespVO>> page(@Validated @RequestBody AppApartmentPageReqVO reqVO) {
        return Result.success(rentalAppApartmentService.pageApartment(reqVO));
    }

    /**
     * 查询已发布公寓详情
     *
     * @param id 公寓 ID
     * @return 公寓详情
     */
    @PermitAll
    @Operation(summary = "查询公寓详情", description = "含费用项与图片；不含房间列表")
    @GetMapping("/getById")
    public Result<AppApartmentRespVO> getById(@Parameter(description = "公寓 ID") @RequestParam Long id) {
        return Result.success(rentalAppApartmentService.getApartment(id));
    }
}
