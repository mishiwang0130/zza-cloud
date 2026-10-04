package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentPageReqVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentRespVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentUpdateStatusReqVO;

/**
 * 看房预约服务：管理端只看列表与流转状态。
 *
 * <p>预约由 App 端发起（本期未实现），管理端不需要新增与删除：看房结果用状态流转表达。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalViewAppointmentService {

    /**
     * 分页查询看房预约列表
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<ViewAppointmentRespVO> pageAppointment(ViewAppointmentPageReqVO reqVO);

    /**
     * 预约状态流转
     *
     * @param reqVO 状态流转入参
     */
    void updateStatus(ViewAppointmentUpdateStatusReqVO reqVO);
}
