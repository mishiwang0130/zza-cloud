package com.wxy.common.webflux.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.ErrorCode;
import com.wxy.common.core.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * 网关全局异常处理：把响应式链路里的异常渲染成与业务服务一致的 {@link Result}。
 *
 * <p>网关直接面向客户端，如果异常响应还是 Spring 默认的错误结构，前端要维护两套解析逻辑，
 * 所以这里把异常统一成 {@code code/msg/data}。
 *
 * <p>HTTP 状态码与业务服务保持一致：鉴权失败 401、路由或资源不存在 404、
 * 其余未捕获异常 500，而业务异常按规范返回 200。
 *
 * <p>响应已提交（例如已经开始回写文件流）时不再插手，直接把异常抛回给后续处理器，
 * 避免出现「写了一半又改状态码」的损坏响应。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Slf4j
public class GlobalWebExceptionHandler implements WebExceptionHandler, Ordered {

    /** 用于序列化统一响应体；网关的 HTTP 层仍使用 Jackson，与业务服务一致 */
    private final ObjectMapper objectMapper;

    /**
     * 构造方法注入 ObjectMapper
     *
     * @param objectMapper Spring Boot 自动配置的 ObjectMapper
     */
    public GlobalWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 异常处理顺序：排在框架默认处理器之前，才能保证统一响应体先生效
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    /**
     * 处理异常并写出统一响应体
     *
     * @param exchange 当前请求上下文
     * @param ex       异常
     * @return 写出响应的完成信号
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatus status;
        ErrorCode errorCode;
        if (ex instanceof UnauthorizedException unauthorized) {
            status = HttpStatus.UNAUTHORIZED;
            errorCode = new ErrorCode(unauthorized.getCode(), unauthorized.getMsg());
        } else if (ex instanceof BizException biz) {
            status = HttpStatus.OK;
            errorCode = new ErrorCode(biz.getCode(), biz.getMsg());
        } else if (ex instanceof ResponseStatusException responseStatusException) {
            status = resolveStatus(responseStatusException);
            errorCode = mapErrorCode(status);
            log.warn("网关请求异常：status={}, msg={}", status.value(), ex.getMessage());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = CommonErrorConstant.SYSTEM_ERROR;
            log.error("网关系统异常", ex);
        }
        return writeResult(exchange, status, errorCode);
    }

    /**
     * 写出统一响应体
     *
     * @param exchange  当前请求上下文
     * @param status    HTTP 状态码
     * @param errorCode 错误码
     * @return 写出响应的完成信号
     */
    private Mono<Void> writeResult(ServerWebExchange exchange, HttpStatus status, ErrorCode errorCode) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Result.error(errorCode));
        } catch (JsonProcessingException e) {
            // 序列化统一响应体都失败时只能结束响应，保证连接不悬挂
            log.error("序列化网关异常响应失败：code={}", errorCode.code(), e);
            return exchange.getResponse().setComplete();
        }
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 解析响应式异常携带的状态码，非法状态码按 500 处理
     *
     * @param ex 响应式异常
     * @return HTTP 状态码
     */
    private static HttpStatus resolveStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }

    /**
     * 把 HTTP 状态码映射成错误码
     *
     * @param status HTTP 状态码
     * @return 错误码
     */
    private static ErrorCode mapErrorCode(HttpStatus status) {
        return switch (status.value()) {
            case 401 -> CommonErrorConstant.UNAUTHORIZED;
            case 403 -> CommonErrorConstant.FORBIDDEN;
            case 404 -> CommonErrorConstant.NOT_FOUND;
            default -> CommonErrorConstant.SYSTEM_ERROR;
        };
    }
}
