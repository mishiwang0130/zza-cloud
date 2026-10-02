package com.wxy.common.redis.config;

import com.wxy.common.redis.util.RedisUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Redis 公共配置：暴露 {@link RedisUtil}。
 *
 * <p>客户端直接用 Spring Boot 自动配置的 {@code StringRedisTemplate}，本模块不再声明
 * {@code RedisTemplate<String, Object>} 与自定义序列化器：Redis 里存的始终是 JSON 字符串，
 * 用 {@code redis-cli} 就能直接看懂，也不存在 JDK 序列化那种改类名就反序列化失败的问题。
 *
 * <p>只有引用 common-redis 的服务才会有这个 Bean，不用 Redis 的服务不受影响。
 *
 * @author wxy
 * @date 2026/10/02
 */
@AutoConfiguration
public class RedisConfig {

    /**
     * 暴露 Redis 常用操作工具
     *
     * @return Redis 操作工具
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisUtil redisUtil() {
        return new RedisUtil();
    }
}
