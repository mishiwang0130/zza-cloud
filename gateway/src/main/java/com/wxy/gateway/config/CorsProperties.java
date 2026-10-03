package com.wxy.gateway.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关跨域配置项，前缀 {@code zza.gateway.cors}。
 *
 * <p>跨域只在网关处理：浏览器把预检请求发给网关，由网关直接应答，业务服务不需要关心跨域。
 * 与运行环境无关的项放 application.yml，允许的来源放 application-{profile}.yml。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@ConfigurationProperties(prefix = "zza.gateway.cors")
public class CorsProperties {

    /**
     * 允许的前端来源，支持通配模式（例如 {@code https://*.example.com}）。
     *
     * <p>用 pattern 而不是 allowedOrigins：允许携带凭证时 {@code allowedOrigins} 不能配 {@code *}，
     * Spring 启动直接报错，而 pattern 形式可以安全地配合 {@code allowCredentials} 使用。
     */
    private List<String> allowedOriginPatterns = List.of("*");

    /** 允许的请求头，{@code *} 表示跟随浏览器预检请求里声明的内容 */
    private List<String> allowedHeaders = List.of("*");

    /** 允许的请求方法 */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD");

    /** 是否允许浏览器携带凭证（Cookie 等）；为 true 时来源必须明确，不能直接配 {@code *} 到 allowedOrigins */
    private boolean allowCredentials = true;

    /** 预检结果缓存秒数，减少 OPTIONS 请求 */
    private Long maxAge = 3600L;
}
