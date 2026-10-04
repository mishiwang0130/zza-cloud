package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wxy.rental.biz.po.RentalViewAppointment;
import com.wxy.rental.biz.vo.admin.ViewAppointmentPageReqVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 看房预约 Mapper：单表读写走 MyBatis-Plus，带条件的列表查询写在
 * {@code resources/mapper/RentalViewAppointmentMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface RentalViewAppointmentMapper extends BaseMapper<RentalViewAppointment> {

    /**
     * 分页查询预约列表
     *
     * <p>返回实体分页（不 join 公寓表）：公寓名由 Service 批量回查后回填，
     * 这样查询本身始终只碰一张表，公寓表结构变化不会牵动这里。
     *
     * @param page  分页参数
     * @param query 过滤条件
     * @return 分页结果
     */
    IPage<RentalViewAppointment> selectAppointmentPage(IPage<RentalViewAppointment> page,
                                                      @Param("query") ViewAppointmentPageReqVO query);
}
