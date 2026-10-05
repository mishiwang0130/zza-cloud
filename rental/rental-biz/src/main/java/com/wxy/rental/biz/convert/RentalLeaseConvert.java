package com.wxy.rental.biz.convert;

import com.wxy.rental.api.dto.LeaseRespDTO;
import com.wxy.rental.biz.po.RentalLease;
import com.wxy.rental.biz.vo.admin.LeaseRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 租约对象转换：实体到详情返回体。
 *
 * <p>需要回填的字段（租客昵称手机、公寓名、房间号、状态中文名）一律 {@code ignore}，由 Service 按需组装。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalLeaseConvert {

    /**
     * 实体转详情返回体（回填字段由 Service 负责）
     *
     * @param po 租约实体，可以为 null
     * @return 详情返回体，入参为 null 时返回 null
     */
    @Mapping(target = "userNickname", ignore = true)
    @Mapping(target = "userMobile", ignore = true)
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "roomNumber", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    LeaseRespVO toRespVO(RentalLease po);

    LeaseRespDTO toRespDTO(RentalLease rentalLease);
}
