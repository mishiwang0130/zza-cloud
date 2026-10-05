package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.api.dto.RoomSearchReqDTO;
import com.wxy.rental.api.dto.RoomSummaryDTO;
import com.wxy.rental.biz.vo.admin.RoomCreateReqVO;
import com.wxy.rental.biz.vo.admin.RoomPageItemRespVO;
import com.wxy.rental.biz.vo.admin.RoomPageReqVO;
import com.wxy.rental.biz.vo.admin.RoomRespVO;
import com.wxy.rental.biz.vo.admin.RoomSimpleRespVO;
import com.wxy.rental.biz.vo.admin.RoomUpdatePublishStatusReqVO;
import com.wxy.rental.biz.vo.admin.RoomUpdateReqVO;
import java.util.List;

/**
 * 房间服务：房间表单（含标签、配套、图片）、列表与上下架。
 *
 * <p>房间没有删除接口：下架即「不再对外展示」，下架前要确认房间没有生效中的租约。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalRoomService {

    /**
     * 新增房间：主表与图片在同一个事务里落库
     *
     * @param reqVO 新增入参
     * @return 新房间 ID
     */
    Long createRoom(RoomCreateReqVO reqVO);

    /**
     * 修改房间：入参为 null 的字段表示不改动，图片按提交的列表覆盖写
     *
     * @param reqVO 修改入参
     */
    void updateRoom(RoomUpdateReqVO reqVO);

    /**
     * 查询房间详情（含公寓名称、标签与配套中文名、图片）
     *
     * @param id 房间 ID
     * @return 房间详情
     */
    RoomRespVO getRoom(Long id);

    /**
     * 分页查询房间列表（含公寓名称与入住状态）
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<RoomPageItemRespVO> pageRoom(RoomPageReqVO reqVO);

    /**
     * 房间上架 / 下架
     *
     * @param reqVO 上架 / 下架入参
     */
    void updatePublishStatus(RoomUpdatePublishStatusReqVO reqVO);

    /**
     * 查询某个公寓下的房间精简信息（租约选房下拉用）
     *
     * @param apartmentId 公寓 ID
     * @return 房间精简列表
     */
    List<RoomSimpleRespVO> listSimpleByApartment(Long apartmentId);

    /**
     * 搜索可用房间
     *
     * @param roomSearchReqDTO 房间搜索要求dto
     * @return {@code List<RoomSummaryDTO> }
     * @author wxy
     * @date 2026/10/05
     */
    List<RoomSummaryDTO> searchAvailableRooms(RoomSearchReqDTO roomSearchReqDTO);
}
