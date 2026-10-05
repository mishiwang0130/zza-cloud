package com.wxy.common.lock.config;

import com.wxy.common.lock.util.DistributedLockUtil;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * 分布式锁自动配置：按 {@code spring.data.redis.*} 装配 Redisson 客户端与加锁工具。
 *
 * <p><b>为什么不用 redisson-spring-boot-starter</b>：那个 starter 会把 spring-data-redis 的
 * 连接工厂替换成 Redisson 的实现，而 {@code common-redis} 的 {@code RedisUtil} 还承担平台凭证缓存读取，
 * 属于鉴权关键路径；这里改成自己构造一个只用于加锁的 {@code RedissonClient}，
 * 原来的 Lettuce 连接与 RedisTemplate 行为完全不变。
 *
 * <p><b>装配条件</b>：只有显式配置了 {@code spring.data.redis.host} 的服务才会创建 Redisson 客户端
 * （Redisson 建连是即时的，没有 Redis 的服务不该在启动时去连一个不存在的地址），
 * 并且可以用 {@code zza.lock.enabled=false} 一键关掉。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(prefix = "zza.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {

    /** 连接池最小空闲连接数：锁是低频操作，够用即可，不需要和业务 Redis 连接池一样大 */
    private static final int CONNECTION_MINIMUM_IDLE_SIZE = 2;

    /** 连接池最大连接数 */
    private static final int CONNECTION_POOL_SIZE = 8;

    /** 连接超时默认值（毫秒）：比 RedisProperties 默认值更短，Redis 不可用时尽早失败 */
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 3000;

    /** 命令超时默认值（毫秒） */
    private static final int DEFAULT_TIMEOUT_MILLIS = 3000;

    /**
     * 创建 Redisson 客户端（仅在配置了 Redis 地址时装配）
     *
     * @param redisProperties Redis 配置（spring.data.redis.*）
     * @return Redisson 客户端，由容器负责在关闭时调用 shutdown
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        SingleServerConfig singleServer = config.useSingleServer()
                .setAddress(buildAddress(redisProperties))
                .setDatabase(redisProperties.getDatabase())
                .setConnectionMinimumIdleSize(CONNECTION_MINIMUM_IDLE_SIZE)
                .setConnectionPoolSize(CONNECTION_POOL_SIZE)
                .setConnectTimeout(toMillis(redisProperties.getConnectTimeout(), DEFAULT_CONNECT_TIMEOUT_MILLIS))
                .setTimeout(toMillis(redisProperties.getTimeout(), DEFAULT_TIMEOUT_MILLIS));
        if (StringUtils.hasText(redisProperties.getUsername())) {
            singleServer.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            singleServer.setPassword(redisProperties.getPassword());
        }
        log.info("分布式锁使用 Redisson，Redis 地址 {}，库号 {}", singleServer.getAddress(), redisProperties.getDatabase());
        return Redisson.create(config);
    }

    /**
     * 创建加锁工具：业务代码只依赖它，不直接依赖 RedissonClient
     *
     * @param redissonClient Redisson 客户端
     * @return 加锁工具
     */
    @Bean
    @ConditionalOnMissingBean
    public DistributedLockUtil distributedLockUtil(RedissonClient redissonClient) {
        return new DistributedLockUtil(redissonClient);
    }

    /**
     * 拼 Redis 地址：开了 SSL 用 rediss 前缀
     *
     * @param redisProperties Redis 配置
     * @return Redisson 可识别的地址
     */
    private String buildAddress(RedisProperties redisProperties) {
        boolean sslEnabled = redisProperties.getSsl() != null && redisProperties.getSsl().isEnabled();
        return (sslEnabled ? "rediss://" : "redis://") + redisProperties.getHost() + ":" + redisProperties.getPort();
    }

    /**
     * Duration 转毫秒：为空或非正数时取默认值
     *
     * @param duration     Spring Boot 3 的 Redis 超时配置
     * @param defaultValue 默认毫秒数
     * @return 毫秒超时
     */
    private int toMillis(Duration duration, int defaultValue) {
        if (Objects.isNull(duration) || duration.isZero() || duration.isNegative()) {
            return defaultValue;
        }
        return (int) Math.min(duration.toMillis(), Integer.MAX_VALUE);
    }
}
