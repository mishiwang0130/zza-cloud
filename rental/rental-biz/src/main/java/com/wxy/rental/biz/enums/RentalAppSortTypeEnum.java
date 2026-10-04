package com.wxy.rental.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * App 房源列表排序方式：公寓 / 房间列表共用一套口径，前端只传数字。
 *
 * <p>只做「排序口径」的白名单与默认值收敛：非法值（含 null）一律归到 {@link #COMPREHENSIVE}，
 * 让「不认识的排序方式」退化成现有排序而不是拼出非法 SQL。具体排序表达式写在 Mapper XML 里，
 * 因为它要落到不同的表与列上（公寓按已发布房间的最低价、房间按自身租金）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum RentalAppSortTypeEnum {

    /** 综合：默认排序，保持列表原有的「按 id 倒序」 */
    COMPREHENSIVE(0, "综合"),

    /** 月租金从低到高 */
    RENT_ASC(1, "月租金从低到高"),

    /** 月租金从高到低 */
    RENT_DESC(2, "月租金从高到低"),

    /** 最新上架：按 create_time 倒序 */
    NEWEST(3, "最新上架");

    /** 传给 Mapper 的排序值 */
    private final Integer value;

    /** 中文描述，仅用于说明与日志 */
    private final String label;

    /**
     * 按值查找枚举
     *
     * @param value 排序值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static RentalAppSortTypeEnum of(Integer value) {
        for (RentalAppSortTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 把排序值收敛成合法值：认识的按原值，其余（null、非法数字）都按综合处理
     *
     * @param value 原始排序值，可以为 null
     * @return 合法的排序值
     */
    public static int normalize(Integer value) {
        RentalAppSortTypeEnum item = of(value);
        return item == null ? COMPREHENSIVE.value : item.value;
    }
}
