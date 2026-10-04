package com.wxy.rental.biz.vo.app;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * App 取消看房预约入参：按接口约定写接口用 POST + 请求体，所以单个 ID 也包成一个对象。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppViewAppointmentCancelReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 预约 ID，必填 */
    @NotNull(message = "预约 ID 不能为空")
    private Long id;
}
