package com.wxy.common.webmvc.interceptor;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.constant.TraceConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 链路追踪拦截器：把网关生成的 traceId 放进 MDC，请求结束后清理。
 *
 * <p>放 MDC 是为了让日志格式一行就能带出来（{@code logging.pattern.level} 里引用
 * {@code TraceConstant.MDC_KEY}），排查问题时能按同一个 id 把网关与各服务的日志串起来。
 *
 * <p>必须在 afterCompletion 清理：Servlet 线程是复用的，MDC 不清会让下一个请求的日志带上
 * 上一个请求的 traceId，两个不相关的请求在日志里连成一条线，比没有 traceId 更难查。
 *
 * <p>请求头缺失时不写入任何值（服务间内部调用、健康检查、绕过网关直连服务都是这种情况）：
 * 日志里的 {@code %X{traceId:-}} 退化成空，而不是凭空造一个查不到上游的 id。
 *
 * @author wxy
 * @date 2026/10/03
 */
public class TraceIdInterceptor implements HandlerInterceptor {

    /**
     * 请求进入 Controller 前把 traceId 放进 MDC
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  处理器
     * @return 恒为 true，追踪信息不影响请求是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(HeaderConstant.TRACE_ID);
        if (StringUtils.hasText(traceId)) {
            MDC.put(TraceConstant.MDC_KEY, traceId);
        }
        return true;
    }

    /**
     * 请求结束后清理 MDC，避免线程复用导致 traceId 串号
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  处理器
     * @param ex       处理过程中的异常，正常结束时为 null
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(TraceConstant.MDC_KEY);
    }
}
