package com.wxy.rental.biz.convert;

import com.wxy.rental.api.dto.ViewAppointmentCreateReqDTO;
import com.wxy.rental.biz.po.RentalViewAppointment;
import com.wxy.rental.biz.vo.app.AppViewAppointmentCreateReqVO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * App 看房预约对象转换：实体到用户端返回体。
 *
 * <p>公寓名与状态中文名由 Service 批量回填，这里 {@code ignore}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalAppAppointmentConvert {


    /**
     * 实体转返回体（回填字段由 Service 负责）
     *
     * @param po 预约实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    @Mapping(target = "apartmentName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    AppViewAppointmentRespVO toRespVO(RentalViewAppointment po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 预约实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<AppViewAppointmentRespVO> toRespVOList(List<RentalViewAppointment> list);

    AppViewAppointmentCreateReqVO toAppViewAppointmentCreateReqVO(ViewAppointmentCreateReqDTO viewAppointmentCreateReqDTO);
}
