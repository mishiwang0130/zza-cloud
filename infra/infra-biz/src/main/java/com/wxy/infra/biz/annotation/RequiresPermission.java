package com.wxy.infra.biz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解：标注在 Controller 的类或方法上，由 {@code PermissionInterceptor} 强制校验。
 *
 * <p>取值引用 {@code InfraPermissionConstant} 中的权限标识，多个值之间是「或」的关系：
 * 命中任意一个即放行，便于一个接口同时接受多种权限（例如列表接口同时允许用户与角色的查询权限）。
 *
 * <p>不标注即放行（只要求登录）：app 端的控制器在权限体系接入前不加该注解即可正常工作。
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
