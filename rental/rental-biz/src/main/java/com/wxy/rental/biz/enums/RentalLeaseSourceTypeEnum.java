package com.wxy.rental.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 租约来源：对应 {@code rental_lease.source_type}，用于区分新签与续约。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum RentalLeaseSourceTypeEnum {

    /** 新签 */
    NEW(1, "新签"),

    /** 续约 */
    RENEW(2, "续约");

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
    public static RentalLeaseSourceTypeEnum of(Integer value) {
        for (RentalLeaseSourceTypeEnum item : values()) {
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
        RentalLeaseSourceTypeEnum item = of(value);
        return item == null ? null : item.label;
    }
}
