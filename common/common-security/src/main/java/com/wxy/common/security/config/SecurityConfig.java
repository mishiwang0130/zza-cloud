package com.wxy.common.security.config;

import com.wxy.common.core.security.PermissionChecker;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.common.security.defaults.DefaultPermissionChecker;
import com.wxy.common.security.defaults.DefaultTokenValidator;
import com.wxy.common.security.util.JwtUtil;
import com.wxy.infra.api.client.InfraPermissionClient;
import com.wxy.infra.api.client.InfraTokenClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

/**
 * 认证能力自动配置：签发/解析 JWT 的原子能力，以及「默认鉴权实现」。
 *
 * <p>只提供签发与解析的原子能力。哪些路径需要登录、白名单怎么配、token 放在哪里，
 * 由网关与各服务自己决定，公共模块不替业务做这个决定。
 *
 * <p>服务需要鉴权时，可以什么都不写，直接用这里的默认实现：
 * {@link DefaultTokenValidator}（平台凭证缓存优先、回源调 infra）与
 * {@link DefaultPermissionChecker}（调 infra 判断权限）；
 * 有自己的用户表/权限数据时，定义同类型的 Bean 覆盖即可（这里都带 {@code @ConditionalOnMissingBean}）。
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
     * <p>只有配置了 {@code zza.jwt.secret} 才装配：不需要签发/解析 JWT 的服务（例如只靠
     * 平台凭证缓存与 infra 校验令牌的服务）不必配一个用不到的密钥；真正注入 {@link JwtUtil} 的服务
     * 如果没配密钥，会在启动时因为找不到该 Bean 而失败，仍然是最早暴露问题。
     *
     * @param jwtProperties JWT 配置
     * @return JWT 工具
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zza.jwt", name = "secret")
    public JwtUtil jwtUtil(JwtProperties jwtProperties) {
        return new JwtUtil(jwtProperties.getSecret(), jwtProperties.getExpireSeconds());
    }

    /**
     * 默认令牌校验实现：服务没有自己的 {@code TokenValidator} 时生效
     *
     * <p>需要 {@code InfraTokenClient}（Feign 客户端），所以服务必须在启动类上开启扫描：
     * {@code @EnableFeignClients(basePackages = "com.wxy.infra.api.client")}；
     * 忘了开就启动失败，报错信息里直接写清楚该加什么。
     *
     * @param redisUtil           Redis 读写工具
     * @param infraTokenProvider  infra 凭证服务客户端
     * @return 默认令牌校验实现
     */
    @Bean
    @ConditionalOnMissingBean
    public TokenValidator defaultTokenValidator(RedisUtil redisUtil,
                                                ObjectProvider<InfraTokenClient> infraTokenProvider) {
        InfraTokenClient infraTokenClient = infraTokenProvider.getIfAvailable();
        if (infraTokenClient == null) {
            throw new IllegalStateException("默认令牌校验实现需要 InfraTokenClient，但容器里没有这个 Bean："
                    + "请确认启动类加了 @EnableFeignClients(basePackages = \"com.wxy.infra.api.client\")；"
                    + "或者自己定义 TokenValidator Bean 覆盖默认实现");
        }
        return new DefaultTokenValidator(redisUtil, infraTokenClient);
    }

    /**
     * 默认权限校验实现：服务没有自己的 {@code PermissionChecker} 时生效
     *
     * @param infraPermissionProvider infra 权限服务客户端
     * @return 默认权限校验实现
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionChecker defaultPermissionChecker(
            ObjectProvider<InfraPermissionClient> infraPermissionProvider) {
        InfraPermissionClient infraPermissionClient = infraPermissionProvider.getIfAvailable();
        if (infraPermissionClient == null) {
            throw new IllegalStateException("默认权限校验实现需要 InfraPermissionClient，但容器里没有这个 Bean："
                    + "请确认启动类加了 @EnableFeignClients(basePackages = \"com.wxy.infra.api.client\")；"
                    + "或者自己定义 PermissionChecker Bean 覆盖默认实现");
        }
        return new DefaultPermissionChecker(infraPermissionClient);
    }
}
