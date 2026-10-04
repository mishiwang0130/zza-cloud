package com.wxy.rental.biz.service;

import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentCreateReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageItemRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentSimpleRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentUpdatePublishStatusReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentUpdateReqVO;
import java.util.List;

/**
 * 公寓服务：公寓表单（含标签、配套、费用项、图片）、列表与上下架。
 *
 * <p>公寓没有删除接口：下架即「不再对外展示」，下架前要确认公寓下没有已发布房间。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalApartmentService {

    /**
     * 新增公寓：主表、费用项关联与图片在同一个事务里落库
     *
     * @param reqVO 新增入参
     * @return 新公寓 ID
     */
    Long createApartment(ApartmentCreateReqVO reqVO);

    /**
     * 修改公寓：入参为 null 的字段表示不改动，费用项与图片按提交的列表覆盖写
     *
     * @param reqVO 修改入参
     */
    void updateApartment(ApartmentUpdateReqVO reqVO);

    /**
     * 查询公寓详情（含区县名、标签 / 配套中文名、费用项与图片）
     *
     * @param id 公寓 ID
     * @return 公寓详情
     */
    ApartmentRespVO getApartment(Long id);

    /**
     * 分页查询公寓列表（含房间总数与空置房间数）
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    PageRespVO<ApartmentPageItemRespVO> pageApartment(ApartmentPageReqVO reqVO);

    /**
     * 公寓上架 / 下架
     *
     * @param reqVO 上架 / 下架入参
     */
    void updatePublishStatus(ApartmentUpdatePublishStatusReqVO reqVO);

    /**
     * 查询全部公寓的精简信息（房间表单的公寓下拉用）
     *
     * @return 公寓精简列表
     */
    List<ApartmentSimpleRespVO> listSimple();
}
