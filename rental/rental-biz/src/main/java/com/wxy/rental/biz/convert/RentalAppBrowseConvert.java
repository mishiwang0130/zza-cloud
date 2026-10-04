package com.wxy.rental.biz.convert;

import com.wxy.rental.biz.po.RentalBrowseHistory;
import com.wxy.rental.biz.vo.app.AppRoomBrowseRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * App 浏览记录对象转换：实体到用户端返回体。
 *
 * <p>房间号、公寓、租金与封面图都由 Service 批量回填，这里 {@code ignore}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalAppBrowseConvert {

    /**
     * 实体转返回体（回填字段由 Service 负责）
     *
     * @param po 浏览记录实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    @Mapping(target = "roomNumber", ignore = true)
    @Mapping(target = "apartmentId", ignore = true)
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "rent", ignore = true)
    @Mapping(target = "coverFileId", ignore = true)
    @Mapping(target = "coverFileUrl", ignore = true)
    AppRoomBrowseRespVO toRespVO(RentalBrowseHistory po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 浏览记录实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<AppRoomBrowseRespVO> toRespVOList(List<RentalBrowseHistory> list);
}
