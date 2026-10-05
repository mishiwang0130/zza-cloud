package com.wxy.rental.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交看房预约的服务间入参：预约人由调用方显式传入，不走请求头。
 *
 * <p>为什么必须显式传：服务间调用不一定在 Web 请求线程上（例如 AI 工具在弹性线程池里执行），
 * 下游读 {@code UserContextHolder} 会拿到空值，只有把 userId 放进契约才能保证预约人有主。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
public class ViewAppointmentCreateReqDTO {

    /**
     * 预约人 ID：由调用方按当前登录用户传入，必填
     */
    @NotNull(message = "预约人不能为空")
    private Long userId;

    /**
     * 公寓名称，必填
     */
    @NotBlank(message = "公寓名称不能为空")
    private String apartmentName;

    /**
     * 预约看房时间，必填
     */
    @NotNull(message = "预约时间不能为空")
    private LocalDateTime appointmentTime;

    /**
     * 备注，可空
     */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
