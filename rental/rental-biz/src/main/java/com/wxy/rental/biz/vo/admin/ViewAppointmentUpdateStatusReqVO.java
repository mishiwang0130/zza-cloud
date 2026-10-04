package com.wxy.rental.biz.vo.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 看房预约状态流转入参：管理端只允许 1 待看房 → 3 已看房 / 2 已取消。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ViewAppointmentUpdateStatusReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 预约 ID，必填 */
    @NotNull(message = "预约 ID 不能为空")
    private Long id;

    /** 目标状态：1 待看房、2 已取消、3 已看房，必填 */
    @NotNull(message = "目标状态不能为空")
    @Min(value = 1, message = "预约状态不合法")
    @Max(value = 3, message = "预约状态不合法")
    private Integer status;
}
