package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.api.dto.RoomDetailDTO;
import com.wxy.rental.biz.vo.app.AppRoomItemRespVO;
import com.wxy.rental.biz.vo.app.AppRoomPageReqVO;
import com.wxy.rental.biz.vo.app.AppRoomRespVO;

/**
 * 用户端房间查询服务：只读，未登录也能访问。
 *
 * <p>房间详情本身就是「一次功能一个接口」：返回详情页要的全部数据，同时自己异步补写浏览记录（发送失败只记日志）。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalAppRoomService {

    /**
     * 分页查询已发布房间（只查已发布公寓下的房间）
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    PageRespVO<AppRoomItemRespVO> pageRoom(AppRoomPageReqVO reqVO);

    /**
     * 查询已发布房间详情（含所属公寓精简信息与图片），并异步补写浏览记录
     *
     * @param id 房间 ID
     * @return 房间详情
     */
    AppRoomRespVO getRoom(Long id);

    /**
     * 获取房间详细信息
     *
     * @param roomNumber 房间号
     * @return {@code RoomDetailDTO }
     * @author wxy
     * @date 2026/10/05
     */
    RoomDetailDTO getRoomDetail(String roomNumber);
}
