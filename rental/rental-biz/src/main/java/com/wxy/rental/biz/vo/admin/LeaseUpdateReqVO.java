package com.wxy.rental.biz.vo.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 租约修改入参：只包含可改的条款，签约主体（租客、公寓、房间）不允许改。
 *
 * <p>状态为 3 已取消 / 4 已到期 / 6 已退租时只能改合同文件与备注：Service 会比对提交值与库里的值，
 * 只要条款真的被改动就报 {@code LEASE_UPDATE_FORBIDDEN}；提交原值不算改动。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class LeaseUpdateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 租约 ID，必填 */
    @NotNull(message = "租约 ID 不能为空")
    private Long id;

    /** 合同文件 ID；不传表示不改动，传 0 表示清空（视为尚未上传） */
    private Long contractFileId;

    /** 租约开始日期；不传表示不改动 */
    private LocalDate leaseStartDate;

    /** 租约结束日期；不传表示不改动 */
    private LocalDate leaseEndDate;

    /** 签约月租金；不传表示不改动 */
    @DecimalMin(value = "0", message = "签约月租金不能为负数")
    private BigDecimal rent;

    /** 押金；不传表示不改动 */
    @DecimalMin(value = "0", message = "押金不能为负数")
    private BigDecimal deposit;

    /** 备注；不传表示不改动 */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
