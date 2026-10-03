package com.wxy.infra.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * infra 凭证配置项，前缀 {@code zza.infra.token}。
 *
 * <p>access token 的有效期不在这里配置：它取自 {@code zza.jwt.expire-seconds}
 * （common-security 的 {@code JwtProperties}），保证网关与本服务对同一个 token 的有效期理解一致。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@ConfigurationProperties(prefix = "zza.infra.token")
public class InfraTokenProperties {

    /** 续期凭证有效期（秒），默认 7 天 */
    private long refreshExpireSeconds = 604800L;

    /** 用户权限集合缓存有效期（秒），默认 30 分钟；授权关系变更时会主动清理 */
    private long permissionCacheSeconds = 1800L;
}
