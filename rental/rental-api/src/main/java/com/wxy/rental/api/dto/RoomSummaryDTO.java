package com.wxy.rental.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomSummaryDTO {
    /**
     * 公寓名称
     */
    private String apartmentName;
    /** 房间号，同一公寓内唯一 */
    private String roomNumber;
    /** 签约月租金（元/月），签约时从房间快照 */
    private BigDecimal rent;
}
