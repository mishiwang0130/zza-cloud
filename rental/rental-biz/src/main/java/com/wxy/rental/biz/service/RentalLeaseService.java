package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.vo.admin.LeaseCreateReqVO;
import com.wxy.rental.biz.vo.admin.LeasePageItemRespVO;
import com.wxy.rental.biz.vo.admin.LeasePageReqVO;
import com.wxy.rental.biz.vo.admin.LeaseRespVO;
import com.wxy.rental.biz.vo.admin.LeaseUpdateReqVO;
import com.wxy.rental.biz.vo.admin.LeaseUpdateStatusReqVO;

/**
 * 租约服务：签约、条款维护与状态流转。
 *
 * <p>租约没有删除接口：要作废就置为 3 已取消；状态流转严格按 {@code RentalLeaseStatusEnum} 的流转表执行。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalLeaseService {

    /**
     * 新增租约
     *
     * @param reqVO 新增入参
     * @return 新租约 ID
     */
    Long createLease(LeaseCreateReqVO reqVO);

    /**
     * 修改租约条款
     *
     * @param reqVO 修改入参
     */
    void updateLease(LeaseUpdateReqVO reqVO);

    /**
     * 查询租约详情
     *
     * @param id 租约 ID
     * @return 租约详情
     */
    LeaseRespVO getLease(Long id);

    /**
     * 分页查询租约列表
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<LeasePageItemRespVO> pageLease(LeasePageReqVO reqVO);

    /**
     * 租约状态流转
     *
     * @param reqVO 状态流转入参
     */
    void updateStatus(LeaseUpdateStatusReqVO reqVO);
}
