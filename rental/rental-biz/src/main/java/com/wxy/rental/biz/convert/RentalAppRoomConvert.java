package com.wxy.rental.biz.convert;

import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.vo.app.AppRoomItemRespVO;
import com.wxy.rental.biz.vo.app.AppRoomRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * App 房间对象转换：实体到用户端返回体。
 *
 * <p>来自公寓的字段（公寓名、区县、押金月数、付款方式、起租月数）与名称类、图片类字段一律 {@code ignore}，由 Service 按需组装。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalAppRoomConvert {

    /**
     * 实体转 App 列表行返回体（回填字段由 Service 负责）
     *
     * @param po 房间实体，可以为 null
     * @return 列表行返回体，入参为 null 时返回 null
     */
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "districtId", ignore = true)
    @Mapping(target = "districtName", ignore = true)
    @Mapping(target = "orientationName", ignore = true)
    @Mapping(target = "depositMonths", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "minLeaseMonths", ignore = true)
    @Mapping(target = "coverFileId", ignore = true)
    @Mapping(target = "coverFileUrl", ignore = true)
    @Mapping(target = "labelCodes", ignore = true)
    @Mapping(target = "facilityCodes", ignore = true)
    AppRoomItemRespVO toItemRespVO(RentalRoom po);

    /**
     * 实体列表转 App 列表行返回体列表
     *
     * @param list 房间实体列表，可以为 null
     * @return 列表行返回体列表，入参为 null 时返回 null
     */
    List<AppRoomItemRespVO> toItemRespVOList(List<RentalRoom> list);

    /**
     * 实体转 App 详情返回体（回填字段由 Service 负责）
     *
     * @param po 房间实体，可以为 null
     * @return 详情返回体，入参为 null 时返回 null
     */
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "districtId", ignore = true)
    @Mapping(target = "districtName", ignore = true)
    @Mapping(target = "orientationName", ignore = true)
    @Mapping(target = "depositMonths", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "minLeaseMonths", ignore = true)
    @Mapping(target = "coverFileId", ignore = true)
    @Mapping(target = "coverFileUrl", ignore = true)
    @Mapping(target = "labelCodes", ignore = true)
    @Mapping(target = "facilityCodes", ignore = true)
    @Mapping(target = "addressDetail", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "introduction", ignore = true)
    @Mapping(target = "feeItems", ignore = true)
    @Mapping(target = "images", ignore = true)
    AppRoomRespVO toRespVO(RentalRoom po);
}
