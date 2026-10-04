package com.wxy.rental.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.constant.RentalPermissionConstant;
import com.wxy.rental.biz.service.RentalApartmentService;
import com.wxy.rental.biz.vo.admin.ApartmentCreateReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageItemRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentSimpleRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentUpdatePublishStatusReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentUpdateReqVO;
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
 * 管理后台公寓接口：路径遵循「资源 + 动作」，最终对外路径为 {@code /api/rental/admin-api/apartment/...}。
 *
 * <p>没有删除接口：下架即「不再对外展示」，见 {@code /updatePublishStatus}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "管理后台 - 公寓")
@RestController
@RequestMapping("/apartment")
public class ApartmentAdminController {

    /** 公寓服务 */
    @Resource
    private RentalApartmentService rentalApartmentService;

    /**
     * 新增公寓
     *
     * @param reqVO 新增入参
     * @return 新公寓 ID
     */
    @Operation(summary = "新增公寓", description = "标签、配套、费用项、图片随表单一次提交，不需要额外的保存接口")
    @RequiresPermission(RentalPermissionConstant.APARTMENT_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody ApartmentCreateReqVO reqVO) {
        return Result.success(rentalApartmentService.createApartment(reqVO));
    }

    /**
     * 修改公寓
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改公寓", description = "入参为 null 的字段表示不改动；费用项与图片按提交的列表覆盖写")
    @RequiresPermission(RentalPermissionConstant.APARTMENT_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody ApartmentUpdateReqVO reqVO) {
        rentalApartmentService.updateApartment(reqVO);
        return Result.success();
    }

    /**
     * 查询公寓详情
     *
     * @param id 公寓 ID
     * @return 公寓详情
     */
    @Operation(summary = "查询公寓详情", description = "含区县名、标签与配套中文名、费用项与图片；不含房间与租约")
    @RequiresPermission(RentalPermissionConstant.APARTMENT_QUERY)
    @GetMapping("/getById")
    public Result<ApartmentRespVO> getById(@Parameter(description = "公寓 ID") @RequestParam Long id) {
        return Result.success(rentalApartmentService.getApartment(id));
    }

    /**
     * 分页查询公寓列表
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询公寓列表", description = "只返回列表字段，额外带房间总数与空置房间数")
    @RequiresPermission(RentalPermissionConstant.APARTMENT_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<ApartmentPageItemRespVO>> page(@Validated @RequestBody ApartmentPageReqVO reqVO) {
        return Result.success(rentalApartmentService.pageApartment(reqVO));
    }

    /**
     * 公寓上架 / 下架
     *
     * @param reqVO 上架 / 下架入参
     * @return 空响应
     */
    @Operation(summary = "公寓上架 / 下架", description = "下架前校验公寓下是否还有已发布房间")
    @RequiresPermission(RentalPermissionConstant.APARTMENT_UPDATE_PUBLISH_STATUS)
    @PostMapping("/updatePublishStatus")
    public Result<Void> updatePublishStatus(@Validated @RequestBody ApartmentUpdatePublishStatusReqVO reqVO) {
        rentalApartmentService.updatePublishStatus(reqVO);
        return Result.success();
    }

    /**
     * 查询全部公寓的精简信息
     *
     * @return 公寓精简列表
     */
    @Operation(summary = "查询公寓下拉列表", description = "房间表单的公寓下拉用，只返回 ID、名称与详细地址")
    @RequiresPermission(RentalPermissionConstant.APARTMENT_QUERY)
    @GetMapping("/listSimple")
    public Result<List<ApartmentSimpleRespVO>> listSimple() {
        return Result.success(rentalApartmentService.listSimple());
    }
}
