package com.wxy.rental.api.dto;

import lombok.Data;

@Data
public class RoomSearchReqDTO {

    /**
     * 城市名称
     */
    private String cityName;

    /**
     * 最低租金
     */
    private Integer minRent;

    /**
     * 最高租金
     */
    private Integer maxRent;

    /**
     * 户型室数
     */
    private Integer roomCount;

    /**
     * 公寓名称
     */
    private String apartmentName;
}
