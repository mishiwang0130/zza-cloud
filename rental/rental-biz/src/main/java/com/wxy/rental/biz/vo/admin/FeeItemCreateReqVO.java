package com.wxy.rental.biz.vo.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 费用项新增入参。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class FeeItemCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 费用项名称，必填，服务内唯一 */
    @NotBlank(message = "费用项名称不能为空")
    @Size(max = 64, message = "费用项名称长度不能超过 64")
    private String name;

    /** 费用金额（元），必填，不允许为负 */
    @NotNull(message = "费用金额不能为空")
    @DecimalMin(value = "0", message = "费用金额不能为负数")
    private BigDecimal amount;

    /** 计价单位，如 吨、度、月；不传按空串处理 */
    @Size(max = 16, message = "计价单位长度不能超过 16")
    private String unit;
}
