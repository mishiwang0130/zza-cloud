package com.wxy.rental.biz.convert;

import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.vo.app.AppApartmentItemRespVO;
import com.wxy.rental.biz.vo.app.AppApartmentRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * App 公寓对象转换：实体到用户端返回体。
 *
 * <p>名称类、聚合类字段（区县名、付款方式中文名、最低租金、封面图、标签中文名、费用项、图片）一律 {@code ignore}，由 Service 按需组装。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalAppApartmentConvert {

    /**
     * 实体转 App 列表行返回体（回填字段由 Service 负责）
     *
     * @param po 公寓实体，可以为 null
     * @return 列表行返回体，入参为 null 时返回 null
     */
    @Mapping(target = "districtName", ignore = true)
    @Mapping(target = "paymentMethodName", ignore = true)
    @Mapping(target = "minRent", ignore = true)
    @Mapping(target = "coverFileId", ignore = true)
    @Mapping(target = "coverFileUrl", ignore = true)
    @Mapping(target = "labelCodes", ignore = true)
    @Mapping(target = "facilityCodes", ignore = true)
    AppApartmentItemRespVO toItemRespVO(RentalApartment po);

    /**
     * 实体列表转 App 列表行返回体列表
     *
     * @param list 公寓实体列表，可以为 null
     * @return 列表行返回体列表，入参为 null 时返回 null
     */
    List<AppApartmentItemRespVO> toItemRespVOList(List<RentalApartment> list);

    /**
     * 实体转 App 详情返回体（回填字段由 Service 负责）
     *
     * @param po 公寓实体，可以为 null
     * @return 详情返回体，入参为 null 时返回 null
     */
    @Mapping(target = "districtName", ignore = true)
    @Mapping(target = "paymentMethodName", ignore = true)
    @Mapping(target = "minRent", ignore = true)
    @Mapping(target = "coverFileId", ignore = true)
    @Mapping(target = "coverFileUrl", ignore = true)
    @Mapping(target = "labelCodes", ignore = true)
    @Mapping(target = "facilityCodes", ignore = true)
    @Mapping(target = "feeItems", ignore = true)
    @Mapping(target = "images", ignore = true)
    AppApartmentRespVO toRespVO(RentalApartment po);
}
