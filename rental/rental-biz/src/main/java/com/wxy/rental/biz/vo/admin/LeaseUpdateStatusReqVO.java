package com.wxy.rental.biz.vo.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 租约状态流转入参。
 *
 * <p>租约没有删除接口：要作废就置为 3 已取消，合同不物理删也不逻辑删。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class LeaseUpdateStatusReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 租约 ID，必填 */
    @NotNull(message = "租约 ID 不能为空")
    private Long id;

    /** 目标状态：1~7，取值见 {@code RentalLeaseStatusEnum}，必填 */
    @NotNull(message = "目标状态不能为空")
    @Min(value = 1, message = "租约状态不合法")
    @Max(value = 7, message = "租约状态不合法")
    private Integer status;
}
