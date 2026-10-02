package com.wxy.common.feign.decoder;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign 错误解码器：把下游返回的非 2xx 响应转成本项目的异常类型。
 *
 * <p>401 说明调用链路里的凭证已经失效，转成 {@link UnauthorizedException}，让上游按重新登录处理；
 * 其余状态统一转成远端调用失败，避免 Feign 原生异常直接冒到业务层。
 *
 * <p>注意：按接口规范，业务失败本身是 HTTP 200 加错误码，不会走到这里；
 * 业务层仍需自行判断响应体里 {@code Result} 的成败。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    /**
     * 解码失败响应
     *
     * @param methodKey Feign 方法标识，用于定位是哪个接口调用失败
     * @param response  下游响应
     * @return 转换后的异常
     */
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 401) {
            log.warn("服务调用鉴权失败：{}", methodKey);
            return new UnauthorizedException();
        }
        log.error("服务调用失败：{}, status={}", methodKey, response.status());
        return new BizException(CommonErrorConstant.REMOTE_CALL_ERROR,
                "调用 " + methodKey + " 失败，状态码 " + response.status());
    }
}
