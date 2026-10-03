package com.wxy.infra.api.config;

import com.wxy.infra.api.client.InfraPermissionClient;
import com.wxy.infra.api.client.InfraTokenClient;
import com.wxy.infra.api.client.fallback.InfraPermissionClientFallbackFactory;
import com.wxy.infra.api.client.fallback.InfraTokenClientFallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * infra 客户端的自动配置：注册 Feign 的降级工厂，并在漏配熔断开关时给出提示。
 *
 * <p>为什么降级工厂要在这里注册成 Bean：Feign 的 {@code fallbackFactory} 是通过容器取的，
 * 而引用方不会扫描 {@code com.wxy.infra} 包，所以只能由 infra-api 自己装配（本类注册在
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
@AutoConfiguration
public class InfraClientAutoConfiguration {

    /**
     * 注册凭证客户端的降级工厂
     *
     * @return 降级工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public InfraTokenClientFallbackFactory infraTokenClientFallbackFactory() {
        return new InfraTokenClientFallbackFactory();
    }

    /**
     * 注册权限客户端的降级工厂
     *
     * @return 降级工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public InfraPermissionClientFallbackFactory infraPermissionClientFallbackFactory() {
        return new InfraPermissionClientFallbackFactory();
    }

    /**
     * 启动检查：用了 infra 的客户端却没打开 {@code feign.sentinel.enabled} 时提醒
     *
     * <p>这个开关没打开时，Spring Cloud 不会用 Sentinel 包装 Feign，注解里的
     * {@code fallbackFactory} 会被静默忽略——降级看起来配了、实际不生效，属于必须提醒的隐性问题。
     * 只在容器里真的存在客户端 Bean（即本服务确实在调 infra）时才提示，避免无关服务被打扰。
     *
     * @param environment   运行环境，用于读取开关
     * @param clientProvider 凭证客户端，用于判断本服务是否在用
     * @return 启动检查
     */
    @Bean
    public ApplicationRunner feignSentinelEnabledCheck(Environment environment,
                                                       ObjectProvider<InfraTokenClient> clientProvider) {
        return args -> {
            if (clientProvider.getIfAvailable() == null) {
                return;
            }
            if (!environment.getProperty("feign.sentinel.enabled", Boolean.class, false)) {
                log.warn("检测到本服务调用了 infra 的客户端，但 feign.sentinel.enabled 未开启，"
                        + "Feign 的熔断降级（fallbackFactory）不会生效，请配置 feign.sentinel.enabled: true");
            }
        };
    }
}
