package com.wxy.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关启动类。
 *
 * <p>网关运行在 WebFlux 栈上，按 {@code /api/{服务名}/**} 的约定把请求转发到
 * {@code lb://{服务名}}，只做路由与后续的鉴权、限流，不承载业务逻辑。
 *
 * <p>服务发现由 Nacos 自动装配接管（Spring Cloud 2023 起不再需要
 * {@code @EnableDiscoveryClient}），注册名即 {@code spring.application.name}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * 启动网关
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
