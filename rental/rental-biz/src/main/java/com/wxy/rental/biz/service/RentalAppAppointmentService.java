package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.api.dto.ViewAppointmentRespDTO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentCreateReqVO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentRespVO;

/**
 * 用户端看房预约服务：提交预约、查自己的预约、取消自己的待看房预约。
 *
 * <p>App 端接口的预约人一律取自登录上下文，不接受前端传入 userId；列表只查自己，取消也只能取消自己的。
 * 服务间调用不在 Web 请求线程上，登录上下文可能为空，所以单独提供一个显式传预约人的重载。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalAppAppointmentService {

    /**
     * 提交看房预约（App 端）：预约人取当前登录上下文
     *
     * <p>只能预约已发布公寓：未发布（含已下架）的公寓对 App 视为不存在，报 {@code APARTMENT_NOT_FOUND}。
     *
     * @param reqVO 预约入参
     * @return 新预约 ID
     */
    Long createAppointment(AppViewAppointmentCreateReqVO reqVO);

    /**
     * 提交看房预约（服务间调用）：预约人由调用方显式传入
     *
     * <p>调用方可能不在请求线程上（AI 工具跑在弹性线程池里），登录上下文取不到用户，
     * 不能用 {@code UserContextHolder} 兜底，必须把 userId 当入参传进来。
     *
     * @param reqVO  预约入参
     * @param userId 预约人 ID，不能为空
     * @return 新预约 ID
     */
    Long createAppointment(AppViewAppointmentCreateReqVO reqVO, Long userId);

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
