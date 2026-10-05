package com.wxy.infra.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * infra 文件上传配置，前缀 {@code zza.infra.file}。
 *
 * <p>三个阈值都按二进制单位（MB = 1024 × 1024），与 {@code spring.servlet.multipart} 的口径一致：
 * 分片阈值决定前端从哪个大小开始走分片接口，分片大小是每一片的固定大小，
 * 单文件上限则限制整文件的总大小。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@ConfigurationProperties(prefix = "zza.infra.file")
public class InfraFileProperties {

    /** 分片阈值：文件不超过它走一次请求的普通上传，超过走分片上传；默认 10MB */
    private DataSize multipartThreshold = DataSize.ofMegabytes(10);

    /**
     * 分片大小：每一片的固定字节数，默认 5MB。
     *
     * <p>对象存储要求除最后一片外每片都不小于 5MiB，配小了会在合并时报 {@code EntityTooSmall}，
     * 所以这里不允许低于 5MiB。
     */
    private DataSize chunkSize = DataSize.ofMegabytes(5);

    /** 单个文件大小上限（分片上传的总大小），默认 200MB，超过在初始化时直接拒绝 */
    private DataSize maxFileSize = DataSize.ofMegabytes(200);

    /** 分片上传会话有效期（秒），默认 1 天；每次初始化与上传分片都会续期 */
    private long sessionExpireSeconds = 86400L;
}
