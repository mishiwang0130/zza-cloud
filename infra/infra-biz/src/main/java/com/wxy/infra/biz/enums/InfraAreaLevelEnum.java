package com.wxy.infra.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 行政区划层级：省 / 市 / 区县三级。
 *
 * <p>层级只用于查询过滤与展示（前端按层决定是否还能继续下钻），区划本身靠 {@code parent_id} 组织成树。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Getter
@AllArgsConstructor
public enum InfraAreaLevelEnum {

    /** 省级（含直辖市、自治区） */
    PROVINCE(1, "省"),

    /** 市级 */
    CITY(2, "市"),

    /** 区县级 */
    DISTRICT(3, "区县");

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
    public static InfraAreaLevelEnum of(Integer value) {
        for (InfraAreaLevelEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
