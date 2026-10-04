package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.vo.admin.RoomPageItemRespVO;
import com.wxy.rental.biz.vo.admin.RoomPageReqVO;
import com.wxy.rental.biz.vo.app.AppRoomPageReqVO;
import java.util.List;
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

    /**
     * 分页查询 App 房间列表（只查已发布公寓下的已发布房间）
     *
     * <p>返回实体分页：公寓名、区县、押金月数等跨表字段与封面图、标签中文名都由 Service 批量组装，
     * 这里的 join 只用于「按公寓的区县 / 市筛选」。
     *
     * @param page        分页参数
     * @param query       过滤条件
     * @param districtIds 按市筛选时由 Service 展开出来的区县 ID 列表；为 null 表示不按市筛选
     * @return 房间分页结果
     */
    IPage<RentalRoom> selectAppRoomPage(IPage<RentalRoom> page,
                                        @Param("query") AppRoomPageReqVO query,
                                        @Param("districtIds") List<Long> districtIds);
}
