package com.wxy.rental.biz.convert;

import com.wxy.rental.biz.po.RentalViewAppointment;
import com.wxy.rental.biz.vo.admin.ViewAppointmentRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 看房预约对象转换：实体到返回体。
 *
 * <p>需要回填的字段（用户昵称、公寓名、状态中文名）一律 {@code ignore}，由 Service 按需组装。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalViewAppointmentConvert {

    /**
     * 实体转返回体（回填字段由 Service 负责）
     *
     * <p>单条转换单独开一个方法，列表转换才会复用它；把 {@code @Mapping} 只写在列表方法上时，
     * MapStruct 会给元素映射告警「目标字段未映射」。
     *
     * @param po 预约实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    @Mapping(target = "userNickname", ignore = true)
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    ViewAppointmentRespVO toRespVO(RentalViewAppointment po);

    /**
     * 实体列表转返回体列表（回填字段由 Service 负责）
     *
     * @param list 预约实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<ViewAppointmentRespVO> toRespVOList(List<RentalViewAppointment> list);
}
