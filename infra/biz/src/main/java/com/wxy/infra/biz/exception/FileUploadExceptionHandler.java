package com.wxy.infra.biz.exception;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 文件上传异常处理器：只处理「上传体积超限」这一种情况。
 *
 * <p>多部件请求在进入 Controller 之前就被容器解析，超限时抛的是
 * {@link MaxUploadSizeExceededException}，不处理会被 common 的兜底处理器当成系统异常返回 500，
 * 对使用者来说没有任何可行动信息。
 *
 * <p>本类只针对具体异常类型，不写兜底分支（兜底仍由 common 的全局异常处理器负责），
 * 并用最高优先级保证在通用兜底之前被匹配到。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class FileUploadExceptionHandler {

    /**
     * 处理上传体积超限：返回 HTTP 400（请求本身不合法），错误码表达具体原因
     *
     * @param ex 上传超限异常
     * @return 失败响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("上传文件超过大小限制：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(InfraErrorConstant.FILE_SIZE_EXCEEDED));
    }
}
