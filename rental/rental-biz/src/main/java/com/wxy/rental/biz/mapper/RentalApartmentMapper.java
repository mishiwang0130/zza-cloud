package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.vo.admin.ApartmentPageItemRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageReqVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 公寓 Mapper：单表读写走 MyBatis-Plus，需要跨表聚合的部分写在
 * {@code resources/mapper/RentalApartmentMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface RentalApartmentMapper extends BaseMapper<RentalApartment> {

    /**
     * 分页查询公寓列表
     *
     * <p>返回 {@code ApartmentPageItemRespVO} 而不是 PO，是因为列表要用到房间总数与空置房间数
     * 两个聚合字段：它们由 SQL 里的子查询算出，PO 上没有对应属性。这里只聚合这两个数字，
     * 不把房间明细带出来——房间明细走房间接口。
     *
     * @param page        分页参数
     * @param query       过滤条件
     * @param districtIds 按市筛选时由 Service 展开出来的区县 ID 列表；为 null 表示不按市筛选
     * @return 分页结果
     */
    IPage<ApartmentPageItemRespVO> selectApartmentPage(IPage<ApartmentPageItemRespVO> page,
                                                      @Param("query") ApartmentPageReqVO query,
                                                      @Param("districtIds") List<Long> districtIds);

    /**
     * 统计公寓下已发布的房间数（公寓下架前校验用）
     *
     * @param apartmentId 公寓 ID
     * @return 已发布房间数
     */
    long countPublishedRooms(@Param("apartmentId") Long apartmentId);
}
