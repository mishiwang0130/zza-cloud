package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wxy.rental.biz.bo.ApartmentMinRentBO;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.vo.admin.ApartmentPageItemRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageReqVO;
import com.wxy.rental.biz.vo.app.AppApartmentPageReqVO;
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

    /**
     * 分页查询 App 公寓列表（只查已发布公寓）
     *
     * <p>返回实体分页：列表要用的区县名、封面图、标签中文名、最低租金都由 Service 按需组装，
     * SQL 里只做「哪些公寓该出现」的过滤（含租金 / 面积 / 室数落到房间表的 EXISTS 条件）。
     *
     * @param page        分页参数
     * @param query       过滤条件
     * @param districtIds 按市筛选时由 Service 展开出来的区县 ID 列表；为 null 表示不按市筛选
     * @return 公寓分页结果
     */
    IPage<RentalApartment> selectAppApartmentPage(IPage<RentalApartment> page,
                                                  @Param("query") AppApartmentPageReqVO query,
                                                  @Param("districtIds") List<Long> districtIds);

    /**
     * 批量查询公寓下已发布房间的最低租金（App 列表的「¥起」价格）
     *
     * @param apartmentIds 公寓 ID 列表
     * @return 每个有已发布房间的公寓一条记录
     */
    List<ApartmentMinRentBO> selectMinRentByApartmentIds(@Param("apartmentIds") List<Long> apartmentIds);
}
