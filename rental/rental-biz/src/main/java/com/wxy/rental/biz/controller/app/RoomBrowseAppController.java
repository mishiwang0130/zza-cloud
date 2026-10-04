package com.wxy.rental.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.service.RentalAppBrowseService;
import com.wxy.rental.biz.vo.app.AppRoomBrowseRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端浏览记录接口：最终对外路径为 {@code /api/rental/app-api/room-browse/...}。
 *
 * <p>只有查询、没有写入：记录由 App 房间详情接口通过 MQ 异步补写（见 {@code RentalBrowseHistoryProducer}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "用户端 - 浏览记录")
@RestController
@RequestMapping("/room-browse")
public class RoomBrowseAppController {

    /** 用户端浏览记录服务 */
    @Resource
    private RentalAppBrowseService rentalAppBrowseService;

    /**
     * 分页查询我的浏览记录
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    @Operation(summary = "分页查询我的浏览记录", description = "只查自己，按浏览时间倒序；每次浏览留一条流水，不去重")
    @PostMapping("/page")
    public Result<PageRespVO<AppRoomBrowseRespVO>> page(@Validated @RequestBody PageReqVO reqVO) {
        return Result.success(rentalAppBrowseService.pageBrowse(reqVO));
    }
}
