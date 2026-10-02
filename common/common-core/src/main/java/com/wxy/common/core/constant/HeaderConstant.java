package com.wxy.common.core.constant;

/**
 * HTTP 请求头常量：网关与各服务之间传递登录上下文时使用的自定义头。
 *
 * <p>这些头由网关在校验凭证后写入，业务服务只读取、不信任外部直接传入：
 * 服务不应直接暴露到公网，否则任何人都能伪造 {@code X-User-Id}。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class HeaderConstant {

    /** 登录用户 ID：网关解析凭证后透传 */
    public static final String USER_ID = "X-User-Id";

    /** 登录端类型：取值见 {@code UserTypeEnum} */
    public static final String USER_TYPE = "X-User-Type";

    /** 登录用户名：仅用于日志与审计展示，不参与鉴权 */
    public static final String USER_NAME = "X-User-Name";

    /** 链路追踪 ID：网关生成，各服务在日志中透传 */
    public static final String TRACE_ID = "X-Trace-Id";

    /** 标准鉴权头：值为 "Bearer " + token */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * 工具类常量类，禁止实例化
     */
    private HeaderConstant() {
    }
}
