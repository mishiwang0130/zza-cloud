package com.wxy.rental.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 付款方式：对应 {@code rental_apartment.payment_method}，随公寓详情与租约展示。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum RentalPaymentMethodEnum {

    /** 月付 */
    MONTHLY(1, "月付"),

    /** 季付 */
    QUARTERLY(2, "季付"),

    /** 半年付 */
    HALF_YEAR(3, "半年付"),

    /** 年付 */
    YEARLY(4, "年付");

    /** 入库值 */
    private final Integer value;

    /** 中文描述，直接展示给前端 */
    private final String label;

    /**
     * 按入库值查找枚举
     *
     * @param value 入库值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static RentalPaymentMethodEnum of(Integer value) {
        for (RentalPaymentMethodEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按入库值取中文描述
     *
     * @param value 入库值，可以为 null
     * @return 中文描述，找不到时返回 null
     */
    public static String labelOf(Integer value) {
        RentalPaymentMethodEnum item = of(value);
        return item == null ? null : item.label;
    }
}
