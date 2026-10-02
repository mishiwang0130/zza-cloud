package com.wxy.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 脱敏工具测试：重点覆盖 null 与过短入参，避免脱敏过程本身抛异常。
 *
 * @author wxy
 * @date 2026/10/02
 */
class DesensitizeUtilTest {

    @Test
    @DisplayName("手机号保留前 3 位与后 4 位")
    void shouldDesensitizeMobile() {
        assertThat(DesensitizeUtil.mobile("13812348000")).isEqualTo("138****8000");
    }

    @Test
    @DisplayName("身份证保留前 6 位与后 4 位")
    void shouldDesensitizeIdCard() {
        assertThat(DesensitizeUtil.idCard("110101199001011234")).isEqualTo("110101********1234");
    }

    @Test
    @DisplayName("邮箱只保留首字符")
    void shouldDesensitizeEmail() {
        assertThat(DesensitizeUtil.email("zhangsan@example.com")).isEqualTo("z***@example.com");
    }

    @Test
    @DisplayName("姓名保留姓氏")
    void shouldDesensitizeName() {
        assertThat(DesensitizeUtil.name("张三")).isEqualTo("张*");
    }

    @Test
    @DisplayName("null 与过短入参原样返回")
    void shouldReturnRawValueWhenTooShort() {
        assertThat(DesensitizeUtil.mobile(null)).isNull();
        assertThat(DesensitizeUtil.mobile("138")).isEqualTo("138");
        assertThat(DesensitizeUtil.idCard("123")).isEqualTo("123");
        assertThat(DesensitizeUtil.bankCard(null)).isNull();
        assertThat(DesensitizeUtil.name(null)).isNull();
    }
}
