package com.wxy.rental.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.service.RentalAppRoomService;
import com.wxy.rental.biz.vo.app.AppRoomItemRespVO;
import com.wxy.rental.biz.vo.app.AppRoomPageReqVO;
import com.wxy.rental.biz.vo.app.AppRoomRespVO;
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
 * 用户端房间接口：最终对外路径为 {@code /api/rental/app-api/room/...}。
 *
 * <p>详情接口除了返回详情数据，还会在登录状态下异步补写一条浏览记录：这样前端不需要再为「记录浏览」调第二个接口，未登录时直接跳过。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "用户端 - 房间")
@RestController
@RequestMapping("/room")
public class RoomAppController {

    /** 用户端房间服务 */
    @Resource
    private RentalAppRoomService rentalAppRoomService;

    /**
     * 分页查询已发布房间
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    @PermitAll
    @Operation(summary = "分页查询房间列表", description = "只返回已发布公寓下的已发布房间，含封面图与标签")
    @PostMapping("/page")
    public Result<PageRespVO<AppRoomItemRespVO>> page(@Validated @RequestBody AppRoomPageReqVO reqVO) {
        return Result.success(rentalAppRoomService.pageRoom(reqVO));
    }

    /**
     * 查询已发布房间详情
     *
     * @param id 房间 ID
     * @return 房间详情
     */
    @PermitAll
    @Operation(summary = "查询房间详情", description = "返回详情页全部数据（含公寓精简信息与图片）；登录状态下异步补写浏览记录")
    @GetMapping("/getById")
    public Result<AppRoomRespVO> getById(@Parameter(description = "房间 ID") @RequestParam Long id) {
        return Result.success(rentalAppRoomService.getRoom(id));
    }
}
