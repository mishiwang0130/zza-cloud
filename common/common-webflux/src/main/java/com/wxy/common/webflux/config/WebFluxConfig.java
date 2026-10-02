package com.wxy.common.webflux.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxy.common.webflux.handler.GlobalWebExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 响应式 Web 公共能力自动配置：注册网关的统一异常处理器。
 *
 * <p>只有引用 common-webflux 的应用（网关）才会生效；
 * 业务服务引用的是 common-webmvc，两者互不干扰。
 *
 * @author wxy
 * @date 2026/10/02
 */
@AutoConfiguration
public class WebFluxConfig {

    /**
     * 注册网关全局异常处理器
     *
     * @param objectMapper Jackson 序列化器
     * @return 全局异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalWebExceptionHandler globalWebExceptionHandler(ObjectMapper objectMapper) {
        return new GlobalWebExceptionHandler(objectMapper);
    }
}
