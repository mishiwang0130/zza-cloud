package com.wxy.common.core.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 统一响应体与错误码校验的测试。
 *
 * @author wxy
 * @date 2026/10/02
 */
class ResultTest {

    @Test
    @DisplayName("成功响应携带成功错误码与业务数据")
    void shouldBuildSuccessResult() {
        Result<String> result = Result.success("data");

        assertThat(result.getCode()).isEqualTo(CommonErrorConstant.SUCCESS.code());
        assertThat(result.getMsg()).isEqualTo(CommonErrorConstant.SUCCESS.msg());
        assertThat(result.getData()).isEqualTo("data");
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("失败响应携带错误码，且可覆盖提示信息")
    void shouldBuildErrorResult() {
        Result<Void> result = Result.error(CommonErrorConstant.PARAM_ERROR);
        assertThat(result.getCode()).isEqualTo(CommonErrorConstant.PARAM_ERROR.code());
        assertThat(result.getMsg()).isEqualTo(CommonErrorConstant.PARAM_ERROR.msg());
        assertThat(result.isSuccess()).isFalse();

        Result<Void> custom = Result.error(CommonErrorConstant.PARAM_ERROR, "手机号格式不正确");
        assertThat(custom.getMsg()).isEqualTo("手机号格式不正确");
    }

    @Test
    @DisplayName("错误码不是 10 位数字时直接报错，避免写出 9 位或八进制码")
    void shouldRejectIllegalErrorCode() {
        assertThatThrownBy(() -> new ErrorCode(20010001, "格式错误"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorCode(CommonErrorConstant.SUCCESS.code(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
