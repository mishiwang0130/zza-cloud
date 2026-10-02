package com.wxy.common.feign.config;

import com.wxy.common.feign.decoder.FeignErrorDecoder;
import com.wxy.common.feign.interceptor.UserContextFeignInterceptor;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Feign 公共能力自动配置：登录上下文透传与统一错误解码。
 *
 * <p>这两类 Bean 全局生效：拦截器作用于所有 Feign 客户端，错误解码器在所有客户端共用，
 * 避免每个 {@code XxxClient} 重复配置。
 *
 * <p>服务需要定制时（例如某个客户端要额外请求头），定义同类型 Bean 即可覆盖默认实现。
 *
 * @author wxy
 * @date 2026/10/02
 */
@AutoConfiguration
public class FeignConfig {

    /**
     * 注册登录上下文透传拦截器
     *
     * @return Feign 请求拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestInterceptor userContextFeignInterceptor() {
        return new UserContextFeignInterceptor();
    }

    /**
     * 注册统一错误解码器
     *
     * @return Feign 错误解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }
}
