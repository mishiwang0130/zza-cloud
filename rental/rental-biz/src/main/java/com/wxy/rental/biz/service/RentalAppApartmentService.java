package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.vo.app.AppApartmentItemRespVO;
import com.wxy.rental.biz.vo.app.AppApartmentPageReqVO;
import com.wxy.rental.biz.vo.app.AppApartmentRespVO;

/**
 * 用户端公寓查询服务：只读，未登录也能访问。
 *
 * <p>与列表、详情的边界一致：列表只给卡片要的字段（含最低租金与封面图），详情再加上介绍、电话、费用项与图片；房间明细由用户端房间接口提供。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalAppApartmentService {

    /**
     * 分页查询已发布公寓（租金 / 面积 / 室数条件落到房间表上过滤）
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    PageRespVO<AppApartmentItemRespVO> pageApartment(AppApartmentPageReqVO reqVO);

    /**
     * 查询已发布公寓详情（含费用项与图片）
     *
     * @param id 公寓 ID
     * @return 公寓详情
     */
    AppApartmentRespVO getApartment(Long id);
}
