package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.vo.admin.RoomPageItemRespVO;
import com.wxy.rental.biz.vo.admin.RoomPageReqVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 房间 Mapper：单表读写走 MyBatis-Plus，需要公寓名与租约派生状态的列表查询写在
 * {@code resources/mapper/RentalRoomMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface RentalRoomMapper extends BaseMapper<RentalRoom> {

    /**
     * 分页查询房间列表
     *
     * <p>返回 {@code RoomPageItemRespVO} 而不是 PO：列表要带公寓名称与入住状态，
     * 前者来自公寓表、后者由租约表派生。两者都是扁平字段，不涉及房间之外的明细数据。
     *
     * @param page  分页参数
     * @param query 过滤条件
     * @return 分页结果
     */
    IPage<RoomPageItemRespVO> selectRoomPage(IPage<RoomPageItemRespVO> page, @Param("query") RoomPageReqVO query);

    /**
     * 统计房间生效中的租约数（房间下架前校验用）
     *
     * <p>只取计数，不带租约明细：下架校验只关心「有没有」，返回整条租约只会让调用方误以为
     * 可以顺手用上租约数据。
     *
     * @param roomId 房间 ID
     * @return 状态为 1/2/5 的租约数
     */
    long countEffectiveLeases(@Param("roomId") Long roomId);
}
