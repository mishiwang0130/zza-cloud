package com.wxy.rental.biz.service;

import com.wxy.rental.biz.vo.admin.FeeItemCreateReqVO;
import com.wxy.rental.biz.vo.admin.FeeItemRespVO;
import com.wxy.rental.biz.vo.admin.FeeItemUpdateReqVO;
import com.wxy.rental.biz.vo.FeeItemSimpleRespVO;
import java.util.List;

/**
 * 费用项服务：公寓杂费的定义维护。
 *
 * <p>费用项字段少，查询只提供 {@code /list}（不分页、不带详情接口），
 * 编辑弹窗直接用列表行数据；删除前要确认没有公寓在引用它。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalFeeItemService {

    /**
     * 新增费用项
     *
     * @param reqVO 新增入参
     * @return 新费用项 ID
     */
    Long createFeeItem(FeeItemCreateReqVO reqVO);

    /**
     * 修改费用项
     *
     * @param reqVO 修改入参
     */
    void updateFeeItem(FeeItemUpdateReqVO reqVO);

    /**
     * 删除费用项（逻辑删除）
     *
     * @param id 费用项 ID
     */
    void deleteFeeItem(Long id);

    /**
     * 查询全部费用项，按创建顺序返回
     *
     * @return 费用项列表
     */
    List<FeeItemRespVO> listFeeItem();

    /**
     * 查询某个公寓包含的费用项（管理端与用户端的公寓详情共用）
     *
     * <p>费用项与公寓的关系只有费用项模块最清楚（关联表由它维护），所以两端的公寓详情都调它，而不是各自再写一遍「查关联表 → 批量查费用项」。
     *
     * @param apartmentId 公寓 ID
     * @return 费用项精简列表，没有关联时返回空列表
     */
    List<FeeItemSimpleRespVO> listByApartmentId(Long apartmentId);
}
