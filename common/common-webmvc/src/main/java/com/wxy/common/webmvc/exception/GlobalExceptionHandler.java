package com.wxy.common.webmvc.exception;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.ErrorCode;
import com.wxy.common.core.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：所有服务共用，把异常统一转成 {@link Result}。
 *
 * <p>HTTP 状态码分工（与接口规范一致）：业务异常返回 200（失败语义由错误码表达）、
 * 未登录返回 401、参数校验失败返回 400、路由资源不存在返回 404、未捕获异常返回 500。
 *
 * <p>由 {@code WebMvcConfig} 装配成 Bean，各服务不需要重复实现，
 * 也不要再写自己的 {@code @RestControllerAdvice} 兜底，否则会出现两套异常行为。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常：返回 HTTP 200，错误码透传给调用方
     *
     * @param ex 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        log.warn("业务异常：code={}, msg={}", ex.getCode(), ex.getMsg());
        return Result.error(new ErrorCode(ex.getCode(), ex.getMsg()));
    }

    /**
     * 处理未登录异常：返回 HTTP 401
     *
     * @param ex 未登录异常
     * @return 失败响应
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("未登录：msg={}", ex.getMsg());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(CommonErrorConstant.UNAUTHORIZED, ex.getMsg()));
    }

    /**
     * 处理请求体参数校验失败：返回 HTTP 400，提示信息取第一个字段错误
     *
     * @param ex 校验失败异常
     * @return 失败响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(CommonErrorConstant.PARAM_ERROR.msg());
        log.warn("参数校验失败：{}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(CommonErrorConstant.PARAM_ERROR, msg));
    }

    /**
     * 处理方法参数上的约束校验失败（类上 {@code @Validated} + 参数上写约束注解）：返回 HTTP 400
     *
     * @param ex 校验失败异常
     * @return 失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(CommonErrorConstant.PARAM_ERROR.msg());
        log.warn("参数校验失败：{}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(CommonErrorConstant.PARAM_ERROR, msg));
    }

    /**
     * 处理方法参数校验失败（Spring 6.1 起 {@code @Validated} 走这条链路）：返回 HTTP 400
     *
     * @param ex 校验失败异常
     * @return 失败响应
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        String msg = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(CommonErrorConstant.PARAM_ERROR.msg());
        log.warn("参数校验失败：{}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(CommonErrorConstant.PARAM_ERROR, msg));
    }

    /**
     * 处理请求体无法解析（JSON 格式错误、字段类型不匹配）：返回 HTTP 400
     *
     * @param ex 解析失败异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(CommonErrorConstant.PARAM_ERROR, "请求体格式不正确"));
    }

    /**
     * 处理路由或静态资源不存在：返回 HTTP 404
     *
     * @param ex 未找到异常
     * @return 失败响应
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFoundException(Exception ex) {
        log.warn("资源不存在：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(CommonErrorConstant.NOT_FOUND));
    }

    /**
     * 兜底处理未捕获异常：返回 HTTP 500
     *
     * <p>日志打完整堆栈便于排查，响应只给通用提示，避免把内部实现细节暴露给调用方。
     *
     * @param ex 未捕获异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(CommonErrorConstant.SYSTEM_ERROR));
    }
}
