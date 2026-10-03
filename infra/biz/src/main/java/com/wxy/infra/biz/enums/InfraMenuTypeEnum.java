package com.wxy.infra.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单类型：区分目录、菜单与按钮，决定前端如何渲染以及后端是否参与接口鉴权。
 *
 * <p>目录与菜单会出现在导航里，按钮只承载权限标识（{@code perms}）不出现在菜单树中。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum InfraMenuTypeEnum {

    /** 目录：导航分组，本身不对应页面 */
    DIR(1, "目录"),

    /** 菜单：对应一个前端页面 */
    MENU(2, "菜单"),

    /** 按钮：不渲染，只提供权限标识 */
    BUTTON(3, "按钮");

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
    public static InfraMenuTypeEnum of(Integer value) {
        for (InfraMenuTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
