package com.wxy.common.core.constant;

/**
 * 链路追踪常量：traceId 在请求头、MDC 与日志格式之间的对应关系。
 *
 * <p>传递链路是：网关为每个请求生成 {@link HeaderConstant#TRACE_ID}（外部传入的同名头一律覆盖），
 * 服务侧由 common-webmvc 的拦截器把它放进 MDC，日志格式再引用 {@link #MDC_KEY} 打出来。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class TraceConstant {

    /**
     * MDC 中的键名。
     *
     * <p>日志格式要写 {@code %X{traceId:-}} 才能取到值，所以改这里必须同步改各服务的
     * {@code logging.pattern.level}，两处保持同一个字符串。
     */
    public static final String MDC_KEY = "traceId";

    /**
     * 工具类常量类，禁止实例化
     */
    private TraceConstant() {
    }
}
