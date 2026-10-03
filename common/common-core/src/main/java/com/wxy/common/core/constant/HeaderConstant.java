package com.wxy.common.core.constant;

/**
 * HTTP 请求头常量：登录上下文与链路追踪在服务之间传递时使用的自定义头。
 *
 * <p><b>谁写这些头</b>：外部请求带来的同名头一律不可信，网关只负责把 {@code X-User-Id}、
 * {@code X-User-Type}、{@code X-User-Name} 删掉（gateway 的 {@code UserHeaderSanitizeFilter}）；
 * 服务之间的 Feign 调用由 common-feign 的拦截器按当前登录用户重新写入。
 * 网关不做登录态注入——登录态以各服务校验令牌的结果为准（common-webmvc 的 {@code TokenAuthInterceptor}）。
 *
 * <p>{@link #TRACE_ID} 走另一条链路：网关为每个请求生成并覆盖外部传入值，各服务只读不写。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class HeaderConstant {

    /** 登录用户 ID：服务间调用（Feign）按当前登录用户写入，网关会删除外部传入的同名头 */
    public static final String USER_ID = "X-User-Id";

    /** 登录端类型：取值见 {@code UserTypeEnum}，由服务间调用透传 */
    public static final String USER_TYPE = "X-User-Type";

    /** 登录用户名：仅用于日志与审计展示，不参与鉴权，由服务间调用透传 */
    public static final String USER_NAME = "X-User-Name";

    /** 链路追踪 ID：网关生成并覆盖外部传入值，各服务写进 MDC 与日志（见 {@code TraceConstant}） */
    public static final String TRACE_ID = "X-Trace-Id";

    /** 标准鉴权头：值为 "Bearer " + token */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * 工具类常量类，禁止实例化
     */
    private HeaderConstant() {
    }
}
