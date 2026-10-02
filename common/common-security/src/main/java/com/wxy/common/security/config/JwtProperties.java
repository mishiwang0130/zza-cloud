package com.wxy.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置项，前缀 {@code zza.jwt}。
 *
 * <p>密钥只能来自环境变量：任何环境都不得把 {@code secret} 写进 YAML 与代码，
 * 本地调试用 {@code application-*.local.yml}（已忽略）或环境变量注入。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Data
@ConfigurationProperties(prefix = "zza.jwt")
public class JwtProperties {

    /** 签名密钥：至少 32 字节（HS256 要求），未配置时启动阶段直接报错 */
    private String secret;

    /** token 有效期（秒），默认 2 小时 */
    private long expireSeconds = 7200L;
}
