package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.api.dto.ViewAppointmentRespDTO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentCreateReqVO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentRespVO;

/**
 * 用户端看房预约服务：提交预约、查自己的预约、取消自己的待看房预约。
 *
 * <p>预约人身份一律取自登录上下文，不接受前端传入 userId；列表只查自己，取消也只能取消自己的。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalAppAppointmentService {

    /**
     * 提交看房预约
     *
     * <p>只能预约已发布公寓：未发布（含已下架）的公寓对 App 视为不存在，报 {@code APARTMENT_NOT_FOUND}。
     *
     * @param reqVO 预约入参
     * @return 新预约 ID
     */
    Long createAppointment(AppViewAppointmentCreateReqVO reqVO);

    /**
     * 分页查询我的看房预约
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    PageRespVO<AppViewAppointmentRespVO> pageAppointment(PageReqVO reqVO);

    /**
     * 取消我的看房预约（只允许待看房的预约）
     *
     * @param id 预约 ID
     */
    void cancelAppointment(Long id);

    ViewAppointmentRespDTO getByUserId(Long userId);
}
