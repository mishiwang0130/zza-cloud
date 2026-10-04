package com.wxy.rental.biz.convert;

import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.vo.admin.ApartmentRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentSimpleRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 公寓对象转换：实体到返回体。
 *
 * <p>需要回填的字段（区县名、字典中文名、付款方式中文名、费用项、图片）一律 {@code ignore}：它们要么来自别的服务、要么来自别的表，必须由 Service 按需组装；MapStruct 静默塞一个 null 反而会掩盖「忘了回填」。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalApartmentConvert {

    /**
     * 实体转详情返回体（回填字段由 Service 负责）
     *
     * @param po 公寓实体，可以为 null
     * @return 详情返回体，入参为 null 时返回 null
     */
    @Mapping(target = "districtName", ignore = true)
    @Mapping(target = "paymentMethodName", ignore = true)
    @Mapping(target = "labelCodes", ignore = true)
    @Mapping(target = "facilityCodes", ignore = true)
    @Mapping(target = "feeItems", ignore = true)
    @Mapping(target = "images", ignore = true)
    ApartmentRespVO toRespVO(RentalApartment po);

    /**
     * 实体列表转精简返回体列表（公寓下拉用）
     *
     * @param list 公寓实体列表，可以为 null
     * @return 精简返回体列表，入参为 null 时返回 null
     */
    List<ApartmentSimpleRespVO> toSimpleRespVOList(List<RentalApartment> list);
}
