package com.wxy.common.core.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解：标注在 Controller 的类或方法上，由公共的权限拦截器强制校验。
 *
 * <p>取值是各服务自己的权限标识（例如 infra 的 {@code infra:user:create}），
 * 由各服务用自己的常量类维护，公共模块不定义具体权限。
 *
 * <p>多个值之间是「或」的关系：命中任意一个即放行，便于一个接口同时接受多种权限。
 * 不标注即不校验权限，只要求登录（登录校验由 {@link TokenValidator} 那套负责）。
 *
 * <p>使用本注解的服务必须提供 {@code PermissionChecker} 实现（权限数据从哪来由服务决定：
 * 自己查库、或调其他服务），否则启动直接失败——错误放在启动阶段暴露，而不是等到线上被调用。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /**
     * 允许访问该接口的权限标识，命中任意一个即通过
     *
     * @return 权限标识数组
     */
    String[] value();
}
