package com.wxy.rental.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 房源图片所属对象类型：对应 {@code rental_image.item_type}。
 *
 * <p>公寓与房间共用一张图片表，靠该字段区分归属；覆盖写图片时按「类型 + 对象 ID」物理删除，
 * 所以这个值必须与查询时用的值严格一致，否则会删不掉旧图、越写越多。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum RentalImageItemTypeEnum {

    /** 公寓图片 */
    APARTMENT(1, "公寓"),

    /** 房间图片 */
    ROOM(2, "房间");

    /** 入库值 */
    private final Integer value;

    /** 中文描述 */
    private final String label;

    /**
     * 按入库值查找枚举
     *
     * @param value 入库值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static RentalImageItemTypeEnum of(Integer value) {
        for (RentalImageItemTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
