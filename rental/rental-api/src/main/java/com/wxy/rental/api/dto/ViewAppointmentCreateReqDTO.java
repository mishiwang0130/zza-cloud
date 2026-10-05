package com.wxy.rental.api.dto;

import lombok.Data;

@Data
public class ViewAppointmentCreateReqDTO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 公寓名称
     */
    private String apartmentName;

    /**
     * 预约时间
     */
    private String appointmentTime;

    /**
     * 备注
     */
    private String remark;
}
