package com.wxy.rental.biz.vo.app;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * App 看房预约提交入参。
 *
 * <p>不填姓名与手机号：预约人就是当前登录用户，落库只存 {@code userId}；
 * 前台要联系看房人时，后台按 {@code userId} 查最新的用户档案，不在这张表里快照联系方式。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppViewAppointmentCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 预约公寓 ID，必填 */
    @NotNull(message = "预约公寓不能为空")
    private Long apartmentId;

    /** 预约看房时间，必填 */
    @NotNull(message = "预约看房时间不能为空")
    private LocalDateTime appointmentTime;

    /** 备注，如「希望看朝南的房间」 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
