package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wxy.rental.biz.po.RentalLease;
import com.wxy.rental.biz.vo.admin.LeasePageItemRespVO;
import com.wxy.rental.biz.vo.admin.LeasePageReqVO;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 租约 Mapper：单表读写走 MyBatis-Plus，需要公寓名与房间号的列表查询、以及到期批量置位写在
 * {@code resources/mapper/RentalLeaseMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface RentalLeaseMapper extends BaseMapper<RentalLease> {

    /**
     * 分页查询租约列表
     *
     * <p>返回 {@code LeasePageItemRespVO} 而不是 PO：列表要带公寓名与房间号，两者都是扁平字段，
     * 一次 join 比逐行回查更省事，也不涉及嵌套对象。
     *
     * @param page  分页参数
     * @param query 过滤条件
     * @return 分页结果
     */
    IPage<LeasePageItemRespVO> selectLeasePage(IPage<LeasePageItemRespVO> page,
                                              @Param("query") LeasePageReqVO query);

    /**
     * 把已签约且已过结束日期的租约批量置为 4 已到期（到期定时任务用）
     *
     * <p>自定义 UPDATE 不会触发 MyBatis-Plus 的审计填充，所以这里显式更新 update_time / update_by。
     * 只更新状态仍为 2 的行，重复执行是安全的（例如多实例同时跑）。
     *
     * @param today    当天日期，结束日期早于它的租约视为已到期
     * @param updateBy 更新人 ID，定时任务传 0（系统）
     * @return 更新行数
     */
    int updateExpiredLeases(@Param("today") LocalDate today, @Param("updateBy") Long updateBy);
}
