package com.wxy.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用启用状态：适用于用户、角色、配置等需要「启用 / 停用」语义的库表字段。
 *
 * <p>约定 {@code 0} 为启用、{@code 1} 为停用，与逻辑删除字段 {@code 0 未删除 / 1 已删除} 保持一致，
 * 避免同一张表里出现两套相反的数字语义。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Getter
@AllArgsConstructor
public enum CommonStatusEnum {

    /** 启用：数据可正常使用 */
    ENABLED(0, "启用"),

    /** 停用：数据保留但不参与业务 */
    DISABLED(1, "停用");

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
    public static CommonStatusEnum of(Integer value) {
        for (CommonStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
