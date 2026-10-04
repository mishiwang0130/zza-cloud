package com.wxy.rental.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发布状态：对应 {@code rental_apartment.publish_status} 与 {@code rental_room.publish_status}。
 *
 * <p>房源没有删除接口，下架（置为 {@link #UNPUBLISHED}）就是「不再对外展示」，
 * 历史数据留在库里供租约与统计追溯。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum RentalPublishStatusEnum {

    /** 未发布：管理端可见，App 端列表不展示 */
    UNPUBLISHED(0, "未发布"),

    /** 已发布：App 端列表可见 */
    PUBLISHED(1, "已发布");

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
    public static RentalPublishStatusEnum of(Integer value) {
        for (RentalPublishStatusEnum item : values()) {
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
        RentalPublishStatusEnum item = of(value);
        return item == null ? null : item.label;
    }
}
