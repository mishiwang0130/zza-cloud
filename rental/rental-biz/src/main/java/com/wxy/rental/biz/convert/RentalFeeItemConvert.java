package com.wxy.rental.biz.convert;

import com.wxy.rental.biz.po.RentalFeeItem;
import com.wxy.rental.biz.vo.admin.FeeItemRespVO;
import com.wxy.rental.biz.vo.admin.FeeItemSimpleRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 费用项对象转换：实体到返回体。
 *
 * <p>新增 / 修改入参转实体不在这里做：名称要去空格、单位要兜默认值，用 MapStruct
 * 反而容易把 null 静默带进库里。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalFeeItemConvert {

    /**
     * 实体转费用项返回体
     *
     * @param po 费用项实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    FeeItemRespVO toRespVO(RentalFeeItem po);

    /**
     * 实体列表转费用项返回体列表
     *
     * @param list 费用项实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<FeeItemRespVO> toRespVOList(List<RentalFeeItem> list);

    /**
     * 实体列表转精简返回体列表（公寓详情内嵌用）
     *
     * @param list 费用项实体列表，可以为 null
     * @return 精简返回体列表，入参为 null 时返回 null
     */
    List<FeeItemSimpleRespVO> toSimpleRespVOList(List<RentalFeeItem> list);
}
