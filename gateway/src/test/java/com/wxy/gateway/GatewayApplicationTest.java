package com.wxy.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.common.core.result.CommonErrorConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 网关启动与响应式链路测试。
 *
 * <p>验证 WebFlux 栈能正常启动、common-webflux 的自动配置（全局异常处理）已生效，
 * 且未匹配任何路由的路径返回 404。测试关闭 Nacos 服务发现，不依赖外部环境。
 *
 * @author wxy
 * @date 2026/10/03
 */
@SpringBootTest(properties = "spring.cloud.nacos.discovery.enabled=false")
@AutoConfigureWebTestClient
class GatewayApplicationTest {

    /** 直连应用上下文的响应式测试客户端，不需要真实端口 */
    @Autowired
    private WebTestClient webTestClient;

    /**
     * 上下文能启动，且测试客户端可用
     */
    @Test
    void contextLoads() {
        assertThat(webTestClient).isNotNull();
    }

    /**
     * 未匹配任何路由的路径返回 404
     */
    @Test
    void unmatchedPathShouldReturnNotFound() {
        webTestClient.get()
                .uri("/api/nope/x")
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * 业务服务的内部端点（actuator、接口文档等）不对外暴露，返回统一响应体的 404
     */
    @Test
    void internalEndpointShouldReturnNotFoundResult() {
        webTestClient.get()
                .uri("/api/infra/actuator/health")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo(CommonErrorConstant.NOT_FOUND.code())
                .jsonPath("$.msg").isEqualTo(CommonErrorConstant.NOT_FOUND.msg());
    }
}
