package com.wxy.ai.agent.biz.constant;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.common.core.result.ErrorCode;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 错误码常量单元测试：守住「10 位数字」「服务位固定 04」「一码一义」三条硬规则。
 *
 * @author wxy
 * @date 2026/10/05
 */
class AiAgentErrorConstantTest {

    /**
     * 所有错误码都必须是 10 位、服务位为 04 的合法值
     */
    @Test
    @DisplayName("错误码：全部为 10 位数字且服务位固定 04")
    void allErrorCodesShouldBeTenDigits() throws IllegalAccessException {
        List<ErrorCode> errorCodes = collectErrorCodes();
        for (ErrorCode errorCode : errorCodes) {
            assertThat(errorCode.code())
                    .as("错误码取值范围：%s", errorCode.msg())
                    .isBetween(ErrorCode.MIN_CODE, ErrorCode.MAX_CODE);
            assertThat(String.valueOf(errorCode.code()))
                    .as("错误码必须 10 位：%s", errorCode.msg())
                    .hasSize(10)
                    .startsWith("104");
        }
        assertThat(errorCodes).isNotEmpty();
    }

    /**
     * 同一份错误码只能对应一种含义，禁止复用；且每个都要有提示信息
     */
    @Test
    @DisplayName("错误码：类内不重复，且每个都有提示信息")
    void allErrorCodesShouldBeUnique() throws IllegalAccessException {
        List<Integer> codes = new ArrayList<>();
        for (ErrorCode errorCode : collectErrorCodes()) {
            assertThat(errorCode.msg()).isNotBlank();
            assertThat(codes).as("错误码重复：%s", errorCode.code()).doesNotContain(errorCode.code());
            codes.add(errorCode.code());
        }
    }

    /**
     * 反射收集常量类里所有的 ErrorCode 字段
     *
     * @return 错误码列表
     */
    private List<ErrorCode> collectErrorCodes() throws IllegalAccessException {
        List<ErrorCode> errorCodes = new ArrayList<>();
        for (Field field : AiAgentErrorConstant.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == ErrorCode.class) {
                errorCodes.add((ErrorCode) field.get(null));
            }
        }
        return errorCodes;
    }
}
