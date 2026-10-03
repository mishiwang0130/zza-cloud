package com.wxy.common.core.result;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
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

    /**
     * 取业务数据，失败时直接抛异常
     *
     * <p>给「调用其他服务」的场景用：远端返回失败（HTTP 200 + 失败 code）时，
     * 调用方通常不想自己判断 {@link #isSuccess()}，而是希望像本地调用一样抛异常——
     * 未登录抛 {@link UnauthorizedException}（对应的 HTTP 状态是 401），其他失败抛 {@link BizException}。
     *
     * <p><b>命名注意</b>：方法名刻意不叫 {@code getCheckedData()}。本类是接口响应体，
     * Jackson 会把 {@code getXxx()} 当成响应字段序列化；那种写法在返回失败响应时
     * 会因为这里的取数据逻辑而抛异常，导致序列化失败、异常处理器失效（踩过一次）。
     * 同类辅助方法一律避免 {@code get}/{@code is} 前缀。
     *
     * @return 业务数据，成功时可能为 null
     * @throws UnauthorizedException 远端返回未登录
     * @throws BizException          远端返回其他失败
     */
    public T requireData() {
        if (isSuccess()) {
            return data;
        }
        if (code == CommonErrorConstant.UNAUTHORIZED.code()) {
            throw new UnauthorizedException(msg);
        }
        throw new BizException(toErrorCode());
    }

    /**
     * 把响应里的 code/msg 还原成错误码
     *
     * <p>远端返回的 code 未必是本项目的 10 位错误码（例如网关或框架直接返回的状态码），
     * 落在合法范围外时退化为系统异常，避免为了报错反而抛出参数非法异常。
     *
     * @return 错误码
     */
    private ErrorCode toErrorCode() {
        if (code < ErrorCode.MIN_CODE || code > ErrorCode.MAX_CODE) {
            return CommonErrorConstant.SYSTEM_ERROR;
        }
        String message = msg == null || msg.isBlank() ? CommonErrorConstant.SYSTEM_ERROR.msg() : msg;
        return new ErrorCode(code, message);
    }
}
