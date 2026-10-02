package com.wxy.common.core.result;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体：所有对外接口的返回值都由它包装。
 *
 * <p>成功与业务异常统一返回 HTTP 200，失败语义全部由 {@link #code} 表达；
 * 只有鉴权失败（401）、参数校验失败（400）、路由资源不存在（404）与未捕获的系统异常（500）
 * 才使用非 200 的 HTTP 状态码。
 *
 * @param <T> 业务数据类型
 * @author wxy
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
public class Result<T> implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误码：成功时为 {@code CommonErrorConstant.SUCCESS}，失败时见对应错误码常量 */
    private int code;

    /** 提示信息：可以直接展示给调用方 */
    private String msg;

    /** 业务数据：失败时为 null */
    private T data;

    /**
     * 构造不带数据的成功响应
     *
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 构造带数据的成功响应
     *
     * @param data 业务数据，可以为 null
     * @param <T>  业务数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(CommonErrorConstant.SUCCESS.code());
        result.setMsg(CommonErrorConstant.SUCCESS.msg());
        result.setData(data);
        return result;
    }

    /**
     * 构造失败响应，提示信息取错误码自带的文案
     *
     * @param errorCode 错误码常量
     * @param <T>       业务数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.msg());
    }

    /**
     * 构造失败响应，并覆盖提示信息
     *
     * <p>需要把上下文（例如具体是哪个字段不合法）告诉调用方时使用；提示信息为空时回退到错误码自带文案。
     *
     * @param errorCode 错误码常量
     * @param msg       自定义提示信息，可以为空
     * @param <T>       业务数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(ErrorCode errorCode, String msg) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.code());
        result.setMsg(msg == null || msg.isBlank() ? errorCode.msg() : msg);
        return result;
    }

    /**
     * 判断是否为成功响应
     *
     * @return 成功返回 true
     */
    public boolean isSuccess() {
        return code == CommonErrorConstant.SUCCESS.code();
    }
}
