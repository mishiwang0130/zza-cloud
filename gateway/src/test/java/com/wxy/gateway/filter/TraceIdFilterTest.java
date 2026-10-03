package com.wxy.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.common.core.constant.HeaderConstant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 链路追踪过滤器测试。
 *
 * <p>断言的重点是「每次请求都有一个新的、不可被客户端左右的 traceId」：客户端自带的同名头必须被覆盖，
 * 同一个 id 同时出现在下游请求头与响应头里。
 *
 * @author wxy
 * @date 2026/10/03
 */
class TraceIdFilterTest {

    /** 被测过滤器 */
    private final TraceIdFilter filter = new TraceIdFilter();

    /**
     * 下游请求头与响应头拿到同一个 traceId
     */
    @Test
    void shouldGenerateTraceIdAndWriteItIntoBothHeaders() {
        ServerWebExchange forwarded = forward(MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/infra/admin-api/user/list").build()));

        String traceId = forwarded.getRequest().getHeaders().getFirst(HeaderConstant.TRACE_ID);
        assertThat(traceId).hasSize(32);
        assertThat(forwarded.getResponse().getHeaders().getFirst(HeaderConstant.TRACE_ID)).isEqualTo(traceId);
    }

    /**
     * 客户端自带的 traceId 必须被覆盖：下游服务直接信任这个头，留着会让日志关联 id 失去可信度
     */
    @Test
    void shouldOverwriteClientSuppliedTraceId() {
        ServerWebExchange forwarded = forward(MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/infra/admin-api/user/list")
                        .header(HeaderConstant.TRACE_ID, "forged-trace-id")
                        .build()));

        assertThat(forwarded.getRequest().getHeaders().getFirst(HeaderConstant.TRACE_ID))
                .isNotEqualTo("forged-trace-id")
                .hasSize(32);
    }

    /**
     * 两次请求的 traceId 不能相同，否则日志里分不清是哪一次
     */
    @Test
    void shouldGenerateDifferentTraceIdPerRequest() {
        String first = forward(MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/infra/admin-api/user/list").build()))
                .getRequest().getHeaders().getFirst(HeaderConstant.TRACE_ID);
        String second = forward(MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/infra/admin-api/user/list").build()))
                .getRequest().getHeaders().getFirst(HeaderConstant.TRACE_ID);

        assertThat(first).isNotEqualTo(second);
    }

    /**
     * 必须排在所有过滤器最前面，后面的环节才能都拿到本次请求的 id
     */
    @Test
    void shouldRunBeforeAnyOtherFilter() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * 执行过滤器并取出真正转发给下游的请求上下文
     *
     * @param exchange 待处理的请求上下文
     * @return 下游收到的请求上下文
     */
    private ServerWebExchange forward(ServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        filter.filter(exchange, target -> {
            forwarded.set(target);
            return Mono.empty();
        }).block();
        assertThat(forwarded.get()).isNotNull();
        return forwarded.get();
    }
}
