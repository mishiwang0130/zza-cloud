package com.wxy.rental.biz.service;

import com.wxy.rental.biz.vo.admin.FeeItemCreateReqVO;
import com.wxy.rental.biz.vo.admin.FeeItemRespVO;
import com.wxy.rental.biz.vo.admin.FeeItemUpdateReqVO;
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
}
