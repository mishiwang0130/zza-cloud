package com.wxy.common.core.exception;

import com.wxy.common.core.result.ErrorCode;
import java.io.Serial;
import lombok.Getter;

/**
 * 业务异常：所有可预期的业务失败都抛它，由全局异常处理器统一转成 {@code Result}。
 *
 * <p>构造时必须传入 {@link ErrorCode} 常量，禁止在业务代码里直接写错误码数字。
 * 业务异常统一返回 HTTP 200，失败语义由错误码表达。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Getter
public class BizException extends RuntimeException {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误码：10 位数字，取自错误码常量 */
    private final int code;

    /** 提示信息：返回给调用方，可以为空（为空时用错误码自带文案） */
    private final String msg;

    /**
     * 用错误码自带文案构造业务异常
     *
     * @param errorCode 错误码常量
     */
    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.msg(), null);
    }

    /**
     * 用自定义文案构造业务异常
     *
     * @param errorCode 错误码常量
     * @param msg       自定义提示信息，可以为空
     */
    public BizException(ErrorCode errorCode, String msg) {
        this(errorCode, msg, null);
    }

    /**
     * 用自定义文案并携带根因构造业务异常
     *
     * @param errorCode 错误码常量
     * @param msg       自定义提示信息，可以为空
     * @param cause     根因异常，用于日志排查
     */
    public BizException(ErrorCode errorCode, String msg, Throwable cause) {
        super(msg == null || msg.isBlank() ? errorCode.msg() : msg, cause);
        this.code = errorCode.code();
        this.msg = getMessage();
    }
}
