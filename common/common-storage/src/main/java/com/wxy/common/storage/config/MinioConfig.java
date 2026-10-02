package com.wxy.common.storage.config;

import com.wxy.common.storage.util.MinioUtil;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * MinIO 自动配置：装配客户端与操作工具。
 *
 * <p>只有配置了 {@code zza.minio.endpoint} 才生效，这样引了 common-storage
 * 但暂时不用对象存储的服务不会因为缺少配置而启动失败。
 *
 * @author wxy
 * @date 2026/10/02
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zza.minio", name = "endpoint")
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    /**
     * 创建 MinIO 客户端，密钥缺失时启动即失败
     *
     * @param minioProperties MinIO 配置
     * @return MinIO 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(MinioProperties minioProperties) {
        if (!StringUtils.hasText(minioProperties.getAccessKey())
                || !StringUtils.hasText(minioProperties.getSecretKey())) {
            throw new IllegalStateException("MinIO 访问密钥未配置，请通过环境变量注入");
        }
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    /**
     * 创建 MinIO 操作工具
     *
     * @param minioClient     MinIO 客户端
     * @param minioProperties MinIO 配置
     * @return MinIO 操作工具
     */
    @Bean
    @ConditionalOnMissingBean
    public MinioUtil minioUtil(MinioClient minioClient, MinioProperties minioProperties) {
        return new MinioUtil(minioClient, minioProperties);
    }
}
