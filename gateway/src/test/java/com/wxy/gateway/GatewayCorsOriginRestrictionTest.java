package com.wxy.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 跨域来源收口测试：模拟生产环境「只允许指定前端来源」的配置。
 *
 * <p>用内联属性覆盖 dev 的放开配置，验证白名单内的来源能过、白名单外的来源被 403 拒绝，
 * 避免生产环境因为配置写错变成任意来源都能带凭证访问。
 *
 * @author wxy
 * @date 2026/10/03
 */
@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "zza.gateway.cors.allowed-origin-patterns=https://admin.example.com"
})
@AutoConfigureWebTestClient
class GatewayCorsOriginRestrictionTest {

    /** 与 {@link GatewayCorsTest} 同理：判断跨域需要请求的 scheme/host，必须用绝对地址 */
    private static final String GATEWAY_BASE_URL = "http://localhost:8080";

    /** 直连应用上下文的响应式测试客户端 */
    @Autowired
    private WebTestClient webTestClient;

    /**
     * 白名单内的来源正常放行
     */
    @Test
    void allowedOriginShouldPass() {
        preflight("https://admin.example.com")
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://admin.example.com");
    }

    /**
     * 白名单外的来源必须被拒绝，且不能回带跨域头
     */
    @Test
    void disallowedOriginShouldBeRejected() {
        preflight("http://evil.example.com")
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    /**
     * 组装一个浏览器预检请求
     *
     * @param origin 前端来源
     * @return 响应断言入口
     */
    private WebTestClient.ResponseSpec preflight(String origin) {
        return webTestClient.options()
                .uri(GATEWAY_BASE_URL + "/api/infra/admin-api/user/list")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange();
    }
}
