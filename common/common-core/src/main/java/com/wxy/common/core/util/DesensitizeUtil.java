package com.wxy.common.core.util;

/**
 * 脱敏工具：日志、导出、外部接口返回敏感信息前统一调用它。
 *
 * <p>所有方法对 null 与过短入参都原样返回，不会抛异常；
 * 脱敏只用于展示，禁止用于需要精确值的场景（如对账、加密验签）。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class DesensitizeUtil {

    /** 掩码字符 */
    private static final String MASK = "*";

    /**
     * 工具类，禁止实例化
     */
    private DesensitizeUtil() {
    }

    /**
     * 手机号脱敏：保留前 3 位与后 4 位，如 138****8000
     *
     * @param mobile 手机号，可以为 null
     * @return 脱敏后的手机号
     */
    public static String mobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + repeat(mobile.length() - 7) + mobile.substring(mobile.length() - 4);
    }

    /**
     * 身份证脱敏：保留前 6 位与后 4 位，如 110101********1234
     *
     * @param idCard 身份证号，可以为 null
     * @return 脱敏后的身份证号
     */
    public static String idCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + repeat(idCard.length() - 10) + idCard.substring(idCard.length() - 4);
    }

    /**
     * 银行卡脱敏：保留前 4 位与后 4 位
     *
     * @param bankCard 银行卡号，可以为 null
     * @return 脱敏后的银行卡号
     */
    public static String bankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + repeat(bankCard.length() - 8) + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 邮箱脱敏：本地部分只保留首字符，如 a***@example.com
     *
     * @param email 邮箱，可以为 null
     * @return 脱敏后的邮箱
     */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + repeat(3) + email.substring(at);
    }

    /**
     * 姓名脱敏：保留姓氏，如 张**
     *
     * @param name 姓名，可以为 null
     * @return 脱敏后的姓名
     */
    public static String name(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + repeat(name.length() - 1);
    }

    /**
     * 生成指定个数的掩码字符
     *
     * @param count 个数，小于 0 时按 0 处理
     * @return 掩码字符串
     */
    private static String repeat(int count) {
        return MASK.repeat(Math.max(count, 0));
    }
}
