package com.wxy.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.common.core.constant.HeaderConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 跨域测试（dev 环境默认配置）。
 *
 * <p>浏览器的预检请求必须由网关直接应答：断言 200 与跨域响应头，同时也就证明了预检没有被
 * 转发给业务服务（真转发的话，测试环境没有 infra 实例，只会拿到 503）。
 *
 * <p>dev 放开所有来源，所以 localhost 与内网 IP 都要能用；生产的收口由
 * {@link GatewayCorsOriginRestrictionTest} 覆盖。
 *
 * @author wxy
 * @date 2026/10/03
 */
@SpringBootTest(properties = "spring.cloud.nacos.discovery.enabled=false")
@AutoConfigureWebTestClient
class GatewayCorsTest {

    /**
     * 测试请求必须用绝对地址：Spring 判断跨域时要拿请求的 scheme/host 与 Origin 比对，
     * 相对路径（例如 /api/infra/...）会让它直接判定 Origin 非法并返回 403
     */
    private static final String GATEWAY_BASE_URL = "http://localhost:8080";

    /** 直连应用上下文的响应式测试客户端 */
    @Autowired
    private WebTestClient webTestClient;

    /**
     * 登录接口的预检请求由网关应答，并回带完整的跨域头
     */
    @Test
    void preflightShouldBeAnsweredByGateway() {
        preflight("/api/infra/admin-api/auth/login", "http://localhost:5173")
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        methods -> assertThat(methods).contains("POST"))
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        headers -> assertThat(headers).containsIgnoringCase("authorization"));
    }

    /**
     * dev 环境前端来源不固定，内网 IP 的来源同样放行
     */
    @Test
    void preflightFromLanOriginShouldBeAnswered() {
        preflight("/api/infra/admin-api/user/list", "http://192.168.205.1:5173")
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://192.168.205.1:5173");
    }

    /**
     * 真实请求（非预检）下浏览器只能读到被 exposed 的响应头：这里验证 traceId 已经放开，
     * 且响应里确实带着网关生成的 id。
     *
     * <p>测试环境没有 infra 实例，路由必然失败（5xx），本用例不关心状态码，
     * 只关心跨域头与 traceId 是否已经写在响应上。
     */
    @Test
    void actualRequestShouldExposeTraceId() {
        webTestClient.get()
                .uri(GATEWAY_BASE_URL + "/api/infra/admin-api/user/list")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        exposed -> assertThat(exposed).containsIgnoringCase(HeaderConstant.TRACE_ID))
                .expectHeader().exists(HeaderConstant.TRACE_ID);
    }

    /**
     * 组装一个浏览器预检请求：OPTIONS + Origin + Access-Control-Request-Method 三者缺一不可，
     * 少任何一个都不算预检请求，会被当成普通请求转发
     *
     * @param path   请求路径
     * @param origin 前端来源
     * @return 响应断言入口
     */
    private WebTestClient.ResponseSpec preflight(String path, String origin) {
        return webTestClient.options()
                .uri(GATEWAY_BASE_URL + path)
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
                .exchange();
    }
}
