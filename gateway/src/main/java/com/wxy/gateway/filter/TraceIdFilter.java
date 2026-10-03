package com.wxy.gateway.filter;

import com.wxy.common.core.constant.HeaderConstant;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 链路追踪过滤器：为每个进入网关的请求生成 traceId，透传给下游并回写到响应头。
 *
 * <p>为什么无条件覆盖外部传入的同名头：{@code X-Trace-Id} 是下游服务直接信任的头，
 * 客户端自带的值可以随便伪造，留着它就等于让日志里的关联 id 失去可信度。
 *
 * <p>响应头也回写一份：前端把 traceId 显示在错误提示里，用户报障时可以直接拿它对日志。
 * 浏览器要读到它，还需要它在跨域配置的 exposedHeaders 里（见 {@code CorsProperties}）。
 *
 * <p>本过滤器排在过滤器链最前面，保证后面的过滤器与下游服务拿到的都是本次请求的 id。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    /** 生成 traceId 时要替换掉的分隔符：32 位无分隔符的十六进制串更适合塞进日志 */
    private static final String UUID_SEPARATOR = "-";

    /**
     * 生成 traceId 写进请求头与响应头后继续执行过滤器链
     *
     * @param exchange 当前请求上下文
     * @param chain    后续过滤器链
     * @return 链路执行完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = newTraceId();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HeaderConstant.TRACE_ID, traceId))
                .build();
        // 响应头在转发前就写好：下游报错时调用方照样能拿到网关的 traceId
        exchange.getResponse().getHeaders().set(HeaderConstant.TRACE_ID, traceId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * 顺序最靠前：后续所有环节都用这个 id，晚于它们生成就没有意义
     *
     * @return 最高优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 生成 traceId
     *
     * @return 32 位无分隔符的十六进制字符串
     */
    private static String newTraceId() {
        return UUID.randomUUID().toString().replace(UUID_SEPARATOR, "");
    }
}
