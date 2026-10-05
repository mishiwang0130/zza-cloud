package com.wxy.rental.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ViewAppointmentRespDTO {
    /** 预约公寓名称，由 Service 批量回填 */
    private String apartmentName;

    /** 预约看房时间 */
    private LocalDateTime appointmentTime;

    /**
     * 未看房总数
     */
    private Integer unViewCount;
}
