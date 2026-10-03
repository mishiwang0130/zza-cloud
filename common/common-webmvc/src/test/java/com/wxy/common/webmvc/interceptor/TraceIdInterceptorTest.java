package com.wxy.common.webmvc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.constant.TraceConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 链路追踪拦截器测试：traceId 进 MDC、请求结束清理，且不影响请求是否放行。
 *
 * @author wxy
 * @date 2026/10/03
 */
class TraceIdInterceptorTest {

    /** 被测拦截器 */
    private final TraceIdInterceptor interceptor = new TraceIdInterceptor();

    /**
     * 每个用例结束后清理，避免用例之间互相影响
     */
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    /**
     * 请求头带 traceId 时写进 MDC，并放行请求
     */
    @Test
    @DisplayName("preHandle：请求头带 traceId 时写进 MDC")
    void shouldPutTraceIdIntoMdc() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderConstant.TRACE_ID, "abc123");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(MDC.get(TraceConstant.MDC_KEY)).isEqualTo("abc123");
    }

    /**
     * 请求头没有 traceId 时不写入：日志里宁可空着，也不能凭空造一个关联不上的 id
     */
    @Test
    @DisplayName("preHandle：没有 traceId 时不动 MDC")
    void shouldNotPutTraceIdWhenHeaderAbsent() {
        interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertThat(MDC.get(TraceConstant.MDC_KEY)).isNull();
    }

    /**
     * 请求结束后必须清理，否则线程复用会让下一个请求的日志带上别人的 traceId
     */
    @Test
    @DisplayName("afterCompletion：清理 MDC")
    void shouldClearMdcAfterCompletion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderConstant.TRACE_ID, "abc123");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertThat(MDC.get(TraceConstant.MDC_KEY)).isNull();
    }
}
