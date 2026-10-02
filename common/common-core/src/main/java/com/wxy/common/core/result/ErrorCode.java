package com.wxy.common.core.result;

/**
 * 错误码：10 位数字，按 {@code PSSMMMEEEE} 分段（项目位 1 位 + 服务位 2 位 + 模块位 3 位 + 错误位 4 位）。
 *
 * <p>示例：{@code 1_00_000_0001} 表示「本项目 - common 服务 - 通用模块 - 参数校验失败」。
 * 同一个错误在所有环境、所有接口下都用同一个错误码，一个错误码只对应一种含义。
 *
 * <p>构造时会校验码值范围，避免写出 9 位、以 0 开头（Java 会当成八进制）这类静默出错的常量。
 *
 * @param code 10 位错误码
 * @param msg  面向调用方的提示信息
 * @author wxy
 * @date 2026/10/02
 */
public record ErrorCode(int code, String msg) {

    /** 项目位固定为 1，10 位错误码的最小值 */
    public static final int MIN_CODE = 1_000_000_000;

    /** 项目位固定为 1，10 位错误码的最大值 */
    public static final int MAX_CODE = 1_999_999_999;

    /**
     * 校验错误码与提示信息，避免非法常量被定义出来
     *
     * @param code 10 位错误码，取值范围 [{@value #MIN_CODE}, {@value #MAX_CODE}]
     * @param msg  提示信息，不能为空
     */
    public ErrorCode {
        if (code < MIN_CODE || code > MAX_CODE) {
            throw new IllegalArgumentException("错误码必须是 10 位数字：" + code);
        }
        if (msg == null || msg.isBlank()) {
            throw new IllegalArgumentException("错误码提示信息不能为空：" + code);
        }
    }
}
