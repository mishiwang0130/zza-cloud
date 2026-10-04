package com.wxy.rental.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 看房预约状态：对应 {@code rental_view_appointment.status}，并承载管理端的流转规则。
 *
 * <p>管理端只允许「1 待看房 → 3 已看房 / 2 已取消」；已看房与已取消都是终态，
 * 这是刻意的：一条预约看完或被取消后再改状态，会让运营看到的接待记录与实际不符。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum RentalAppointmentStatusEnum {

    /** 待看房 */
    PENDING(1, "待看房"),

    /** 已取消 */
    CANCELED(2, "已取消"),

    /** 已看房 */
    VIEWED(3, "已看房");

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
    public static RentalAppointmentStatusEnum of(Integer value) {
        for (RentalAppointmentStatusEnum item : values()) {
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
        RentalAppointmentStatusEnum item = of(value);
        return item == null ? null : item.label;
    }

    /**
     * 判断本状态能否流转到目标状态
     *
     * @param target 目标状态，可以为 null
     * @return 允许流转时返回 true
     */
    public boolean canTransitionTo(RentalAppointmentStatusEnum target) {
        if (target == null) {
            return false;
        }
        return this == PENDING && (target == VIEWED || target == CANCELED);
    }
}
