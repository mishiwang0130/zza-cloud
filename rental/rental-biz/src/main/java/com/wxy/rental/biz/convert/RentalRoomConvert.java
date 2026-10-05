package com.wxy.rental.biz.convert;

import com.wxy.rental.api.dto.RoomDetailDTO;
import com.wxy.rental.api.dto.RoomSummaryDTO;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.vo.admin.RoomRespVO;
import com.wxy.rental.biz.vo.admin.RoomSimpleRespVO;
import java.util.List;

import com.wxy.rental.biz.vo.app.AppRoomRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 房间对象转换：实体到返回体。
 *
 * <p>需要回填的字段（公寓名、字典中文名、图片）一律 {@code ignore}，由 Service 按需组装。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalRoomConvert {

    /**
     * 实体转详情返回体（回填字段由 Service 负责）
     *
     * @param po 房间实体，可以为 null
     * @return 详情返回体，入参为 null 时返回 null
     */
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "labelCodes", ignore = true)
    @Mapping(target = "facilityCodes", ignore = true)
    @Mapping(target = "images", ignore = true)
    RoomRespVO toRespVO(RentalRoom po);

    /**
     * 实体列表转精简返回体列表（选房下拉用）
     *
     * @param list 房间实体列表，可以为 null
     * @return 精简返回体列表，入参为 null 时返回 null
     */
    List<RoomSimpleRespVO> toSimpleRespVOList(List<RentalRoom> list);

    RoomDetailDTO toRoomDetailDTO(RentalRoom rentalRoom);
}
