package com.wxy.common.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置项，前缀 {@code zza.minio}。
 *
 * <p>只有配置了 {@code endpoint} 的应用才会装配 MinIO 客户端，没用对象存储的服务
 * 即使引了 common-storage 也不会有多余 Bean。
 *
 * <p>访问密钥只能来自环境变量，禁止写进 YAML 与代码。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Data
@ConfigurationProperties(prefix = "zza.minio")
public class MinioProperties {

    /** 服务地址，如 http://127.0.0.1:9000；为空时不装配 MinIO 客户端 */
    private String endpoint;

    /** 访问密钥 ID，从环境变量读取 */
    private String accessKey;

    /** 访问密钥，从环境变量读取 */
    private String secretKey;

    /** 默认存储桶名称 */
    private String bucket;

    /** 预签名下载链接的有效期（秒），默认 1 小时 */
    private int presignedExpirySeconds = 3600;
}
