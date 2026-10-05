package com.wxy.rental.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author wxy
 * @description 租约
 * @date 2026/10/05
 */
@Data
public class LeaseRespDTO {

    /** 公寓名称 */
    private String apartmentName;

    /** 房间号，同一公寓内唯一 */
    private String roomNumber;
    /** 租约开始日期 */
    private LocalDate leaseStartDate;

    /** 租约结束日期 */
    private LocalDate leaseEndDate;

    /** 签约月租金（元/月），签约时从房间快照 */
    private BigDecimal rent;

    /** 押金（元），不传时按 租金 × 公寓押金月数 计算 */
    private BigDecimal deposit;
}
