package com.wxy.rental.biz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

/**
 * 依赖守卫测试：Feign 客户端走服务发现（{@code lb://}）时，classpath 上必须有 LoadBalancer。
 *
 * <p>守的是启动期的坑：少一个 {@code spring-cloud-starter-loadbalancer}，Spring Cloud OpenFeign 会在建
 * {@code InfraDictDataClient} 这类 Bean 时抛
 * {@code No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-loadbalancer?}，
 * 整个服务起不来（租户名：rental 启用了 {@code @EnableFeignClients}，见 {@code RentalApplication}）。
 *
 * <p>不启 Spring 上下文是有意的：Nacos、MySQL、Redis 是否可用跟这条依赖约束无关，一旦走上下文，
 * 这个测试就会因为环境而红，反而没人再信它。
 *
 * @author wxy
 * @date 2026/10/04
 */
class RentalFeignLoadBalancerDependencyTest {

    /**
     * LoadBalancer 的自动配置类必须在 classpath 上
     */
    @Test
    @DisplayName("依赖：Feign 需要 spring-cloud-starter-loadbalancer")
    void loadBalancerShouldBeOnClasspath() {
        assertThat(ClassUtils.isPresent(
                "org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration", null))
                .as("缺少 spring-cloud-starter-loadbalancer 时，rental 启动会在创建 Feign 客户端时直接失败")
                .isTrue();
    }
}
