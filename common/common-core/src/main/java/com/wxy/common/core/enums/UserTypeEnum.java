package com.wxy.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录端类型：说明当前登录用户来自管理后台还是用户端。
 *
 * <p>两端的接口路径前缀不同（{@code /admin-api} 与 {@code /app-api}），
 * 越权校验（管理后台接口不接受用户端身份）依赖该枚举区分。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {

    /** 管理后台用户 */
    ADMIN(1, "管理后台"),

    /** 用户端用户 */
    APP(2, "用户端");

    /** 入库值，同时写入 JWT 载荷与请求头 */
    private final Integer value;

    /** 中文描述，用于日志与页面展示 */
    private final String label;

    /**
     * 按值查找枚举
     *
     * @param value 取值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static UserTypeEnum of(Integer value) {
        for (UserTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
