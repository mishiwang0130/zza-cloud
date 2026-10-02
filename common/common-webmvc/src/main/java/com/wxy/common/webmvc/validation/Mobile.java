package com.wxy.common.webmvc.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 手机号校验注解：值为空或符合中国大陆手机号格式时通过。
 *
 * <p>空值算通过是有意为之：是否必填由 {@code @NotBlank} 单独表达，
 * 把两个语义塞进一条注解会让「必填提示」和「格式提示」分不清楚。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MobileValidator.class)
public @interface Mobile {

    /**
     * 校验失败时的提示信息
     *
     * @return 提示信息
     */
    String message() default "手机号格式不正确";

    /**
     * 校验分组
     *
     * @return 分组数组
     */
    Class<?>[] groups() default {};

    /**
     * 校验载荷
     *
     * @return 载荷数组
     */
    Class<? extends Payload>[] payload() default {};
}
