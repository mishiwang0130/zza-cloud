package com.wxy.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 内部端点拦截过滤器测试。
 *
 * <p>覆盖三类路径：必须拦下的内部端点（含各服务的 actuator 与接口文档）、
 * 必须放行的业务路径、以及不属于网关前缀的路径。
 *
 * @author wxy
 * @date 2026/10/03
 */
class InternalEndpointBlockFilterTest {

    /** 被测过滤器 */
    private final InternalEndpointBlockFilter filter = new InternalEndpointBlockFilter();

    /**
     * infra 的 actuator、接口文档数据、文档页面都必须返回 404
     */
    @Test
    void shouldBlockInfraInternalEndpoints() {
        assertBlocked("/api/infra/actuator");
        assertBlocked("/api/infra/actuator/health");
        assertBlocked("/api/infra/actuator/metrics/jvm.memory.used");
        assertBlocked("/api/infra/doc.html");
        assertBlocked("/api/infra/v3/api-docs");
        assertBlocked("/api/infra/v3/api-docs/swagger-config");
        assertBlocked("/api/infra/swagger-ui/index.html");
        assertBlocked("/api/infra/webjars/js/app.js");
        assertBlocked("/api/infra/favicon.ico");
    }

    /**
     * 拦截按「服务名之后的第一段」匹配，新增服务不需要改代码
     */
    @Test
    void shouldBlockInternalEndpointsOfEveryService() {
        assertBlocked("/api/zza/actuator/health");
        assertBlocked("/api/ai-agent/actuator/health");
        assertBlocked("/api/ai-agent/v3/api-docs");
    }

    /**
     * 正常业务路径必须放行，不能被误伤
     */
    @Test
    void shouldPassThroughBusinessPaths() {
        assertPassed("/api/infra/admin-api/user/list");
        assertPassed("/api/infra/app-api/user/getById");
        assertPassed("/api/zza/admin-api/order/list");
        // 业务路径里出现同名片段不受影响：只比对服务名之后的第一段
        assertPassed("/api/infra/admin-api/doc.html/list");
        assertPassed("/api/infra/actuator-x/health");
    }

    /**
     * 不是「网关前缀 + 服务名」结构的路径不属于网关路由范围，本过滤器不处理
     */
    @Test
    void shouldIgnorePathsOutsideGatewayPrefix() {
        assertPassed("/actuator/health");
        assertPassed("/doc.html");
        assertPassed("/api");
        assertPassed("/api/infra");
    }

    /**
     * 必须排在请求头清洗之后、转发之前
     */
    @Test
    void shouldRunBeforeRouting() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    /**
     * 断言路径被拦下：抛 404，且不执行后续过滤器链
     *
     * @param path 请求路径
     */
    private void assertBlocked(String path) {
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());

        Mono<Void> result = filter.filter(exchange, target -> {
            chainInvoked.set(true);
            return Mono.empty();
        });

        assertThatThrownBy(result::block)
                .as("路径 %s 应当返回 404", path)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(chainInvoked).as("路径 %s 不应继续执行过滤器链", path).isFalse();
    }

    /**
     * 断言路径被放行：不抛异常且执行后续过滤器链
     *
     * @param path 请求路径
     */
    private void assertPassed(String path) {
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());

        filter.filter(exchange, target -> {
            chainInvoked.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainInvoked).as("路径 %s 应当被放行", path).isTrue();
    }
}
