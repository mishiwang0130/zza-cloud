package com.wxy.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.common.core.constant.HeaderConstant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 登录用户请求头清洗过滤器测试。
 *
 * <p>断言的重点是「外部伪造的 X-User-* 一定到不了下游」：无论大小写怎么写都要删掉，
 * 同时不能误删 {@code Authorization} 这类真正需要透传给业务服务的头。
 *
 * @author wxy
 * @date 2026/10/03
 */
class UserHeaderSanitizeFilterTest {

    /** 被测过滤器 */
    private final UserHeaderSanitizeFilter filter = new UserHeaderSanitizeFilter();

    /**
     * 客户端伪造的登录用户头必须全部被删除，业务头保持不变
     */
    @Test
    void shouldRemoveForgedUserHeaders() {
        var requestBuilder = MockServerHttpRequest.get("/api/infra/admin-api/user/list")
                .header(HeaderConstant.USER_ID, "5")
                .header(HeaderConstant.USER_TYPE, "1")
                .header(HeaderConstant.USER_NAME, "forged")
                .header(HeaderConstant.AUTHORIZATION, "Bearer token");

        HttpHeaders forwarded = captureForwardedHeaders(MockServerWebExchange.from(requestBuilder.build()));

        assertThat(forwarded)
                .doesNotContainKey(HeaderConstant.USER_ID)
                .doesNotContainKey(HeaderConstant.USER_TYPE)
                .doesNotContainKey(HeaderConstant.USER_NAME);
        assertThat(forwarded.getFirst(HeaderConstant.AUTHORIZATION)).isEqualTo("Bearer token");
    }

    /**
     * 请求头大小写可以随意书写，清洗不能漏
     */
    @Test
    void shouldRemoveUserHeadersWhateverTheCase() {
        var requestBuilder = MockServerHttpRequest.get("/api/infra/admin-api/user/list")
                .header("x-user-id", "5")
                .header("X-USER-TYPE", "1")
                .header("x-user-name", "forged");

        HttpHeaders forwarded = captureForwardedHeaders(MockServerWebExchange.from(requestBuilder.build()));

        assertThat(forwarded)
                .doesNotContainKey(HeaderConstant.USER_ID)
                .doesNotContainKey(HeaderConstant.USER_TYPE)
                .doesNotContainKey(HeaderConstant.USER_NAME);
    }

    /**
     * 没有伪造头时也必须正常放行，路径等信息不被破坏
     */
    @Test
    void shouldForwardRequestWhenNoForgedHeader() {
        var requestBuilder = MockServerHttpRequest.get("/api/infra/admin-api/user/list?pageNum=1");

        ServerWebExchange exchange = MockServerWebExchange.from(requestBuilder.build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, target -> {
            forwarded.set(target);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getPath().value()).isEqualTo("/api/infra/admin-api/user/list");
        assertThat(forwarded.get().getRequest().getQueryParams().getFirst("pageNum")).isEqualTo("1");
    }

    /**
     * 必须排在所有过滤器最前面，否则清洗可能晚于鉴权或转发
     */
    @Test
    void shouldRunBeforeAnyOtherFilter() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    /**
     * 执行过滤器并取出真正转发给下游的请求头
     *
     * @param exchange 待处理的请求上下文
     * @return 下游收到的请求头
     */
    private HttpHeaders captureForwardedHeaders(ServerWebExchange exchange) {
        AtomicReference<HttpHeaders> captured = new AtomicReference<>();
        filter.filter(exchange, target -> {
            captured.set(target.getRequest().getHeaders());
            return Mono.empty();
        }).block();
        assertThat(captured.get()).isNotNull();
        return captured.get();
    }
}
