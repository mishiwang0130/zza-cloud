package com.wxy.rental.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ViewAppointmentCreateReqDTO {
    /**
     * 用户ID
     */
    @NotNull(message = "用户id不能未空")
    private Long userId;

    /**
     * 公寓名称
     */
    @NotBlank(message = "公寓名称不能未空")
    private String apartmentName;

    /**
     * 预约时间
     */
    @NotBlank(message = "预约时间不能未空")
    private LocalDateTime appointmentTime;

    /**
     * 备注
     */
    private String remark;
}
