package com.wxy.rental.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 房间入住状态：由租约表派生，不入库。
 *
 * <p>房间列表要展示「空置 / 在租」，但这个状态不是房间自己的字段——它随租约变化，
 * 落库就得靠定时任务或事件去同步，容易不一致；因此每次查询按租约的生效状态现算。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum RentalRoomCheckInStatusEnum {

    /** 空置：没有状态为 1/2/5 的租约 */
    VACANT(0, "空置"),

    /** 在租：存在状态为 1/2/5 的租约 */
    RENTED(1, "在租");

    /** 派生值 */
    private final Integer value;

    /** 中文描述 */
    private final String label;

    /**
     * 按值查找枚举
     *
     * @param value 派生值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static RentalRoomCheckInStatusEnum of(Integer value) {
        for (RentalRoomCheckInStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
