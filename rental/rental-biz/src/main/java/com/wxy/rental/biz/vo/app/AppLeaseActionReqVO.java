package com.wxy.rental.biz.vo.app;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * App 租约动作入参：确认签约、申请退租、申请续约三个动作都只需要租约 ID。
 *
 * <p>目标状态由各自的接口决定，不由前端传：让前端传状态等于把状态机的入口开放给客户端，一旦传错就出现非法流转。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppLeaseActionReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 租约 ID，必填 */
    @NotNull(message = "租约 ID 不能为空")
    private Long id;
}
