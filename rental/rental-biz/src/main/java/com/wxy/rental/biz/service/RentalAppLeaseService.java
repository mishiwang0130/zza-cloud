package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.vo.app.AppLeaseItemRespVO;
import com.wxy.rental.biz.vo.app.AppLeaseRespVO;

/**
 * 用户端租约服务：看自己的租约，并处理「确认签约 / 申请退租 / 申请续约」三个由用户本人发起的动作。
 *
 * <p>只允许操作自己的租约：别人的租约一律按「租约不存在」返回，避免用 ID 试探出别人的租约是否存在。
 *
 * <p>三个动作各自固定目标状态，并且只允许从契约稿约定的起始状态出发（1→2、2→5、2→7）：5→2（驳回退租）、7→2（续约完成或驳回）是运营动作，不能由用户本人触发。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalAppLeaseService {

    /**
     * 分页查询我的租约
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    PageRespVO<AppLeaseItemRespVO> pageLease(PageReqVO reqVO);

    /**
     * 查询我的租约详情（含合同文件地址与房间图片）
     *
     * @param id 租约 ID
     * @return 租约详情
     */
    AppLeaseRespVO getLease(Long id);

    /**
     * 确认签约：1 签约待确认 → 2 已签约
     *
     * @param id 租约 ID
     */
    void confirm(Long id);

    /**
     * 申请退租：2 已签约 → 5 退租待确认
     *
     * @param id 租约 ID
     */
    void applyWithdraw(Long id);

    /**
     * 申请续约：2 已签约 → 7 续约待确认
     *
     * @param id 租约 ID
     */
    void applyRenew(Long id);
}
