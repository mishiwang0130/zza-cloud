package com.wxy.infra.biz.constant;

/**
 * infra 服务通用常量：不依赖任何中间件，业务代码统一引用这里，禁止写魔法值。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class InfraConstant {

    /**
     * 超级管理员角色编码：拥有该角色的用户跳过权限校验，且不允许被删除、停用或重置密码。
     */
    public static final String SUPER_ADMIN_ROLE_CODE = "super_admin";

    /**
     * 响应体里的凭证类型，取值固定为 {@code Bearer}
     */
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    /**
     * 续期凭证的随机字节数：32 字节随机数经 Base64 URL 编码后足够抗碰撞。
     */
    public static final int REFRESH_TOKEN_RANDOM_BYTES = 32;

    /**
     * 工具类常量类，禁止实例化
     */
    private InfraConstant() {
    }
}
