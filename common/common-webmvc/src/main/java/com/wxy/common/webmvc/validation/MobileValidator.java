package com.wxy.common.webmvc.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * {@link Mobile} 的校验实现：匹配 1 开头、第二位为 3-9 的 11 位手机号。
 *
 * @author wxy
 * @date 2026/10/02
 */
public class MobileValidator implements ConstraintValidator<Mobile, String> {

    /** 中国大陆手机号格式 */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 校验手机号：空值视为通过，非空时必须是合法手机号
     *
     * @param value   待校验的值
     * @param context 校验上下文
     * @return 通过返回 true
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return MOBILE_PATTERN.matcher(value).matches();
    }
}
