package com.wxy.gateway.config;

import jakarta.annotation.Resource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 网关跨域配置：注册 {@link CorsWebFilter}，由网关直接应答预检请求，不转发给业务服务。
 *
 * <p>跨域放在网关而不是各业务服务：业务服务只在内网被网关调用，自己再配一遍跨域既重复，
 * 也容易出现「预检请求没带 token 被鉴权拦截器拦下」这类问题。
 *
 * <p>用 WebFilter 而不是 Spring Cloud Gateway 的 {@code spring.cloud.gateway.globalcors}：
 * 后者的配置项在 4.1.x 里识别不到（IDE 会提示属性不存在），而 {@code CorsWebFilter} 是 WebFlux
 * 的标准做法，配置项也是本工程自己的 {@code zza.gateway.cors}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    /** 跨域配置项 */
    @Resource
    private CorsProperties corsProperties;

    /**
     * 注册跨域过滤器
     *
     * <p>预检请求（OPTIONS + Origin + Access-Control-Request-Method）由过滤器直接应答 200，
     * 不会进入网关的过滤器链与路由，因此不需要下游服务可用。
     *
     * @return 跨域过滤器
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setMaxAge(corsProperties.getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }
}
