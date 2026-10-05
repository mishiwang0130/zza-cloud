package com.wxy.common.core.result;

/**
 * 错误码：10 位数字，按 {@code PSSMMMEEEE} 分段（项目位 1 位 + 服务位 2 位 + 模块位 3 位 + 错误位 4 位）。
 *
 * <p>示例：{@code 1_00_000_0001} 表示「本项目 - common 服务 - 通用模块 - 参数校验失败」。
 * 同一个错误在所有环境、所有接口下都用同一个错误码，一个错误码只对应一种含义。
 *
 * <p>成功码 {@code 200} 是唯一例外：成功不是「错误」，不占 {@code PSSMMMEEEE} 号段，
 * 这里与 HTTP 200 对齐，接口正常返回时一眼就能看出是成功。
 *
 * <p>构造时会校验码值范围，避免写出 9 位、以 0 开头（Java 会当成八进制）这类静默出错的常量。
 *
 * @param code 10 位错误码
 * @param msg  面向调用方的提示信息
 * @author wxy
 * @date 2026/10/02
 */
public record ErrorCode(int code, String msg) {

    /** 成功码：不占 10 位错误码号段，固定与 HTTP 200 对齐，业务成功时统一返回它 */
    public static final int SUCCESS_CODE = 200;

    /** 项目位固定为 1，10 位错误码的最小值 */
    public static final int MIN_CODE = 1_000_000_000;

    /** 项目位固定为 1，10 位错误码的最大值 */
    public static final int MAX_CODE = 1_999_999_999;

    /**
     * 校验错误码与提示信息，避免非法常量被定义出来
     *
     * <p>取值范围 [{@value #MIN_CODE}, {@value #MAX_CODE}]，另放行成功码 {@value #SUCCESS_CODE}：
     * 只对成功码开一个口子，其余小于 10 位的码值仍然直接报错，防止把 9 位或八进制码写进来。
     *
     * @param code 10 位错误码，取值范围 [{@value #MIN_CODE}, {@value #MAX_CODE}]，或成功码 {@value #SUCCESS_CODE}
     * @param msg  提示信息，不能为空
     */
    public ErrorCode {
        if (code != SUCCESS_CODE && (code < MIN_CODE || code > MAX_CODE)) {
            throw new IllegalArgumentException("错误码必须是 10 位数字（成功码 " + SUCCESS_CODE + " 除外）：" + code);
        }
        if (msg == null || msg.isBlank()) {
            throw new IllegalArgumentException("错误码提示信息不能为空：" + code);
        }
    }
}
