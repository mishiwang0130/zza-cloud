package com.wxy.infra.api.constant;

/**
 * infra 对外发布的接口常量：服务名与服务间接口前缀。
 *
 * <p>前缀固定 {@code /rpc-api}，与端前缀 {@code /admin-api}、{@code /app-api} 并列，
 * 语义是「服务之间通过 OpenFeign 调用的接口」：调用方经 Nacos 服务发现直连 infra，不经过网关。
 * 也正因为不经网关，网关必须把 {@code /api/infra/rpc-api/**} 挡在外面，否则外部顺着服务名路由就能打进来。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class InfraApiConstant {

    /** Nacos 中注册的服务名：Feign 客户端按它做服务发现 */
    public static final String SERVICE_NAME = "infra";

    /** 服务间接口前缀 */
    public static final String RPC_API_PREFIX = "/rpc-api";

    /** 凭证相关接口前缀 */
    public static final String TOKEN_API_PREFIX = RPC_API_PREFIX + "/auth";

    /** 权限相关接口前缀 */
    public static final String PERMISSION_API_PREFIX = RPC_API_PREFIX + "/permission";

    /** 校验访问令牌的路径 */
    public static final String TOKEN_CHECK_PATH = "/check";

    /** 判断用户是否拥有任意一个权限的路径 */
    public static final String PERMISSION_HAS_ANY_PATH = "/has-any";

    /**
     * 工具类常量类，禁止实例化
     */
    private InfraApiConstant() {
    }
}
