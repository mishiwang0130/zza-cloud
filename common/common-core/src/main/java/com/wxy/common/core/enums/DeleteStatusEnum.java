package com.wxy.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 逻辑删除状态：对应库表公共字段 {@code is_delete}。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Getter
@AllArgsConstructor
public enum DeleteStatusEnum {

    /** 未删除：正常数据，查询默认只返回该状态 */
    NOT_DELETED(0, "未删除"),

    /** 已删除：逻辑删除标记，数据仍在库中 */
    DELETED(1, "已删除");

    /** 入库值 */
    private final Integer value;

    /** 中文描述，用于日志与页面展示 */
    private final String label;

    /**
     * 按入库值查找枚举
     *
     * @param value 入库值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static DeleteStatusEnum of(Integer value) {
        for (DeleteStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
