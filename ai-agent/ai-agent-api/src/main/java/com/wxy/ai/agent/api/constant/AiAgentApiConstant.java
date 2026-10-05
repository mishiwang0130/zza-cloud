package com.wxy.ai.agent.api.constant;

/**
 * ai-agent 对外发布的常量：目前只有 Nacos 服务名。
 *
 * <p>服务名写在 api 模块里是为了让它只定义一次：{@code ai-agent-biz} 的
 * {@code spring.application.name}（yml 里只能写字符串）与网关的 {@code lb://ai-agent} 路由
 * 都必须是同一个值。改这里的值时，记得同步 biz 的 yml 与网关路由。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class AiAgentApiConstant {

    /**
     * Nacos 中注册的服务名：不带 {@code -biz} 后缀，网关按 {@code lb://ai-agent} 路由
     * {@code /api/ai-agent/**}。
     */
    public static final String SERVICE_NAME = "ai-agent";

    /**
     * 工具类常量类，禁止实例化
     */
    private AiAgentApiConstant() {
    }
}
