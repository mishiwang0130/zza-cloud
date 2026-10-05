package com.wxy.rental.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

//租金、配套
@Data
public class RoomDetailDTO {
    /** 房间号，同一公寓内唯一 */
    private String roomNumber;
    /** 签约月租金（元/月），签约时从房间快照 */
    private BigDecimal rent;
    /** 配套 */
    private String labels;
}
