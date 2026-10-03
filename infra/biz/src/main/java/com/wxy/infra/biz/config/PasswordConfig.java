package com.wxy.infra.biz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置：统一使用 BCrypt。
 *
 * <p>这里只引 {@code spring-security-crypto} 的加密能力，不引整套 Spring Security，
 * 因此不会产生过滤器链，鉴权仍由网关与本服务的拦截器负责。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Configuration
public class PasswordConfig {

    /**
     * 注册密码编码器：BCrypt 自带随机盐，同一明文每次加密结果都不同，库里无法反推明文
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
