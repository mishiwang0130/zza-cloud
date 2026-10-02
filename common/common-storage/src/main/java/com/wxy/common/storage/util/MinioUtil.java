package com.wxy.common.storage.util;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.storage.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.time.Duration;

/**
 * MinIO 操作工具：业务代码通过它上传、下载、删除文件与生成预签名链接。
 *
 * <p>桶名默认取自配置，也允许显式传入，便于把不同类型的文件分桶存放。
 *
 * <p>MinIO SDK 的方法会抛出一长串受检异常（网络、签名、服务端错误等），
 * 这里统一捕获并包装成业务异常，避免每个调用点都写一遍多重 catch。
 *
 * @author wxy
 * @date 2026/10/02
 */
public class MinioUtil {

    /** MinIO 客户端 */
    private final MinioClient minioClient;

    /** MinIO 配置，提供默认桶名与预签名有效期 */
    private final MinioProperties minioProperties;

    /**
     * 构造方法注入客户端与配置
     *
     * @param minioClient     MinIO 客户端
     * @param minioProperties MinIO 配置
     */
    public MinioUtil(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * 上传文件到默认桶
     *
     * @param objectName  对象名（含目录前缀），如 {@code avatar/2026/10/02/xxx.png}
     * @param stream      文件流，由调用方负责关闭
     * @param size        文件字节数，未知时传 -1（SDK 会分片上传）
     * @param contentType 文件类型，如 {@code image/png}
     */
    public void putObject(String objectName, InputStream stream, long size, String contentType) {
        putObject(minioProperties.getBucket(), objectName, stream, size, contentType);
    }

    /**
     * 上传文件到指定桶
     *
     * @param bucket      桶名
     * @param objectName  对象名
     * @param stream      文件流，由调用方负责关闭
     * @param size        文件字节数，未知时传 -1
     * @param contentType 文件类型
     */
    public void putObject(String bucket, String objectName, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(stream, size, size < 0 ? PutObjectArgs.MAX_PART_SIZE : -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "文件上传失败", ex);
        }
    }

    /**
     * 从默认桶下载文件
     *
     * @param objectName 对象名
     * @return 文件流，由调用方负责关闭
     */
    public InputStream getObject(String objectName) {
        return getObject(minioProperties.getBucket(), objectName);
    }

    /**
     * 从指定桶下载文件
     *
     * @param bucket     桶名
     * @param objectName 对象名
     * @return 文件流，由调用方负责关闭
     */
    public InputStream getObject(String bucket, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "文件下载失败", ex);
        }
    }

    /**
     * 删除默认桶中的文件
     *
     * @param objectName 对象名
     */
    public void removeObject(String objectName) {
        removeObject(minioProperties.getBucket(), objectName);
    }

    /**
     * 删除指定桶中的文件
     *
     * @param bucket     桶名
     * @param objectName 对象名
     */
    public void removeObject(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "文件删除失败", ex);
        }
    }

    /**
     * 生成默认有效期的预签名下载链接，供前端直接下载，避免中转流量
     *
     * @param objectName 对象名
     * @return 预签名链接
     */
    public String presignedGetUrl(String objectName) {
        return presignedGetUrl(objectName, Duration.ofSeconds(minioProperties.getPresignedExpirySeconds()));
    }

    /**
     * 生成指定有效期的预签名下载链接
     *
     * @param objectName 对象名
     * @param expiry     有效期，MinIO 允许的最长有效期为 7 天
     * @return 预签名链接
     */
    public String presignedGetUrl(String objectName, Duration expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .expiry((int) expiry.toSeconds())
                    .build());
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "生成下载链接失败", ex);
        }
    }

    /**
     * 确保默认桶存在，不存在则创建；应用启动或首次上传前调用一次即可
     */
    public void ensureBucket() {
        String bucket = minioProperties.getBucket();
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "存储桶初始化失败", ex);
        }
    }
}
