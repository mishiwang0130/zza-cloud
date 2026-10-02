package com.wxy.common.security.config;

import com.wxy.common.security.util.JwtUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 认证能力自动配置：读取 {@code zza.jwt} 配置并暴露 {@link JwtUtil}。
 *
 * <p>只提供签发与解析的原子能力。哪些路径需要登录、白名单怎么配、token 放在哪里，
 * 由网关与各服务自己决定，公共模块不替业务做这个决定。
 *
 * @author wxy
 * @date 2026/10/02
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * 创建 JWT 工具，密钥或有效期配置不合法时应用启动即失败
     *
     * @param jwtProperties JWT 配置
     * @return JWT 工具
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil(JwtProperties jwtProperties) {
        return new JwtUtil(jwtProperties.getSecret(), jwtProperties.getExpireSeconds());
    }
}
