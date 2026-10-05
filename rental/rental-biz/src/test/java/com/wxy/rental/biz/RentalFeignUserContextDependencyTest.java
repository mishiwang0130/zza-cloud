package com.wxy.rental.biz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

/**
 * 依赖守卫测试：rental 调 infra 的 Feign 请求要带上登录用户，classpath 上必须有 common-feign。
 *
 * <p>守的是「静默失效」的坑：少了 common-feign，服务照样能启动、接口照样能调通，
 * 但请求里不会带 {@code X-User-Id}，下游按登录用户填的审计字段会全部退化成系统用户 0，
 * 事后只能从日志里反查。拦截器与错误解码器都是自动配置，所以这里只断言类在 classpath 上。
 *
 * @author wxy
 * @date 2026/10/06
 */
class RentalFeignUserContextDependencyTest {

    /**
     * 登录上下文透传拦截器必须在 classpath 上
     */
    @Test
    @DisplayName("依赖：Feign 透传登录上下文需要 common-feign")
    void userContextInterceptorShouldBeOnClasspath() {
        assertThat(ClassUtils.isPresent(
                "com.wxy.common.feign.interceptor.UserContextFeignInterceptor", null))
                .as("缺少 common-feign 时，rental 调 infra 的请求不带 X-User-Id，下游审计字段会退化成系统用户")
                .isTrue();
        assertThat(ClassUtils.isPresent("com.wxy.common.feign.config.FeignConfig", null))
                .as("common-feign 的自动配置类不在 classpath 上时，拦截器不会被注册")
                .isTrue();
    }
}
