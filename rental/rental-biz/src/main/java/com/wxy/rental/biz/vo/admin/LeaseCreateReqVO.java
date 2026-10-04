package com.wxy.rental.biz.vo.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 租约新增入参。
 *
 * <p>押金可以不传：不传时按「租金 × 公寓押金月数」计算，避免运营每次都要自己乘一遍。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class LeaseCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 承租人 App 用户 ID，必填 */
    @NotNull(message = "承租人不能为空")
    private Long userId;

    /** 签约公寓 ID，必填 */
    @NotNull(message = "签约公寓不能为空")
    private Long apartmentId;

    /** 签约房间 ID，必填 */
    @NotNull(message = "签约房间不能为空")
    private Long roomId;

    /** 合同文件 ID；不传或传 0 表示尚未上传 */
    private Long contractFileId;

    /** 租约开始日期，必填 */
    @NotNull(message = "租约开始日期不能为空")
    private LocalDate leaseStartDate;

    /** 租约结束日期，必填，必须晚于开始日期 */
    @NotNull(message = "租约结束日期不能为空")
    private LocalDate leaseEndDate;

    /** 签约月租金（元/月），必填 */
    @NotNull(message = "签约月租金不能为空")
    @DecimalMin(value = "0", message = "签约月租金不能为负数")
    private BigDecimal rent;

    /** 押金（元）；不传按「租金 × 公寓押金月数」计算 */
    @DecimalMin(value = "0", message = "押金不能为负数")
    private BigDecimal deposit;

    /** 租约来源：1 新签、2 续约；不传按新签处理 */
    @Min(value = 1, message = "租约来源不合法")
    @Max(value = 2, message = "租约来源不合法")
    private Integer sourceType;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
