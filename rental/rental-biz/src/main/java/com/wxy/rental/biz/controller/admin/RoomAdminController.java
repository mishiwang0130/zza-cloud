package com.wxy.rental.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.constant.RentalPermissionConstant;
import com.wxy.rental.biz.service.RentalRoomService;
import com.wxy.rental.biz.vo.admin.RoomCreateReqVO;
import com.wxy.rental.biz.vo.admin.RoomPageItemRespVO;
import com.wxy.rental.biz.vo.admin.RoomPageReqVO;
import com.wxy.rental.biz.vo.admin.RoomRespVO;
import com.wxy.rental.biz.vo.admin.RoomSimpleRespVO;
import com.wxy.rental.biz.vo.admin.RoomUpdatePublishStatusReqVO;
import com.wxy.rental.biz.vo.admin.RoomUpdateReqVO;
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
 * 管理后台房间接口：路径遵循「资源 + 动作」，最终对外路径为 {@code /api/rental/admin-api/room/...}。
 *
 * <p>没有删除接口：下架即「不再对外展示」，见 {@code /updatePublishStatus}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "管理后台 - 房间")
@RestController
@RequestMapping("/room")
public class RoomAdminController {

    /** 房间服务 */
    @Resource
    private RentalRoomService rentalRoomService;

    /**
     * 新增房间
     *
     * @param reqVO 新增入参
     * @return 新房间 ID
     */
    @Operation(summary = "新增房间", description = "标签、配套、图片随表单一次提交；房间号在同一公寓内唯一")
    @RequiresPermission(RentalPermissionConstant.ROOM_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody RoomCreateReqVO reqVO) {
        return Result.success(rentalRoomService.createRoom(reqVO));
    }

    /**
     * 修改房间
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改房间", description = "入参为 null 的字段表示不改动；图片按提交的列表覆盖写")
    @RequiresPermission(RentalPermissionConstant.ROOM_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody RoomUpdateReqVO reqVO) {
        rentalRoomService.updateRoom(reqVO);
        return Result.success();
    }

    /**
     * 查询房间详情
     *
     * @param id 房间 ID
     * @return 房间详情
     */
    @Operation(summary = "查询房间详情", description = "含公寓名称、标签与配套中文名、图片；不含租约")
    @RequiresPermission(RentalPermissionConstant.ROOM_QUERY)
    @GetMapping("/getById")
    public Result<RoomRespVO> getById(@Parameter(description = "房间 ID") @RequestParam Long id) {
        return Result.success(rentalRoomService.getRoom(id));
    }

    /**
     * 分页查询房间列表
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询房间列表", description = "只返回列表字段，额外带公寓名称与入住状态")
    @RequiresPermission(RentalPermissionConstant.ROOM_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<RoomPageItemRespVO>> page(@Validated @RequestBody RoomPageReqVO reqVO) {
        return Result.success(rentalRoomService.pageRoom(reqVO));
    }

    /**
     * 房间上架 / 下架
     *
     * @param reqVO 上架 / 下架入参
     * @return 空响应
     */
    @Operation(summary = "房间上架 / 下架", description = "下架前校验房间是否存在生效中的租约")
    @RequiresPermission(RentalPermissionConstant.ROOM_UPDATE_PUBLISH_STATUS)
    @PostMapping("/updatePublishStatus")
    public Result<Void> updatePublishStatus(@Validated @RequestBody RoomUpdatePublishStatusReqVO reqVO) {
        rentalRoomService.updatePublishStatus(reqVO);
        return Result.success();
    }

    /**
     * 查询某个公寓下的房间精简信息
     *
     * @param apartmentId 公寓 ID
     * @return 房间精简列表
     */
    @Operation(summary = "查询房间下拉列表", description = "租约表单选房用，只返回 ID、房间号、租金与发布状态")
    @RequiresPermission(RentalPermissionConstant.ROOM_QUERY)
    @GetMapping("/listSimpleByApartment")
    public Result<List<RoomSimpleRespVO>> listSimpleByApartment(
            @Parameter(description = "公寓 ID") @RequestParam Long apartmentId) {
        return Result.success(rentalRoomService.listSimpleByApartment(apartmentId));
    }
}
