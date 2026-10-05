package com.wxy.ai.agent.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.wxy.ai.agent.biz.config.AiAgentProperties;

/**
 * ai-agent 服务启动类：小程序端智能客服对话（SSE）与管理端知识库、会话记录。
 *
 * <p>注册到 Nacos 后服务名为 {@code ai-agent}（见 {@code AiAgentApiConstant.SERVICE_NAME}），
 * 网关按 {@code lb://ai-agent} 转发 {@code /api/ai-agent/**}。对外路径由 common-webmvc
 * 按 Controller 所在包自动加端前缀：{@code controller/app} → {@code /app-api}，
 * {@code controller/admin} → {@code /admin-api}。
 *
 * <p><b>为什么要开 Feign 客户端扫描</b>：本服务不维护用户与权限数据，鉴权直接用 common-security 的
 * 默认实现（{@code DefaultTokenValidator} / {@code DefaultPermissionChecker}），它们依赖
 * {@code com.wxy.infra.api.client} 下的 Feign 客户端回源 infra；组件扫描同时覆盖
 * {@code com.wxy.infra.api}，因为这些客户端的降级工厂是 {@code @Component}
 * （扫描不到时 Feign 会因为找不到降级工厂直接启动失败）。
 *
 * <p>Mapper 扫描范围限定在本服务自己的 {@code mapper} 包；common 模块的自动配置由
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 生效，
 * 不需要把 {@code com.wxy.common} 加进扫描范围。
 *
 * @author wxy
 * @date 2026/10/05
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.wxy.infra.api.client", "com.wxy.rental.api.client"})
@EnableConfigurationProperties(AiAgentProperties.class)
@SpringBootApplication(scanBasePackages = {"com.wxy.ai.agent.biz", "com.wxy.infra.api", "com.wxy.rental.api"})
@MapperScan("com.wxy.ai.agent.biz.mapper")
public class AiAgentApplication {

    /**
     * 服务入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }
}
