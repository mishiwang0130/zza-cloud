package com.wxy.common.storage.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.storage.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.CreateMultipartUploadResponse;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListPartsResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.UploadPartResponse;
import io.minio.http.Method;
import io.minio.messages.Part;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.util.StringUtils;

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

    /**
     * 分片操作等待超时（秒）。
     *
     * <p>单片 5MiB、服务端到 MinIO 走内网，120 秒足够；加超时是为了避免 MinIO 无响应时
     * 请求线程被无限挂住。
     */
    private static final long MULTIPART_TIMEOUT_SECONDS = 120L;

    /** 单次 listParts 请求的分片数上限，MinIO 服务端上限为 1000 */
    private static final int LIST_PARTS_PAGE_SIZE = 1000;

    /** MinIO 客户端 */
    private final MinioClient minioClient;

    /** 分片上传客户端：minio 8.5.12 的分片 API 是 protected，由 {@link MinioMultipartClient} 重新暴露 */
    private final MinioMultipartClient multipartClient;

    /** MinIO 配置，提供默认桶名与预签名有效期 */
    private final MinioProperties minioProperties;

    /**
     * 构造方法注入客户端与配置
     *
     * @param minioClient      MinIO 客户端
     * @param minioAsyncClient MinIO 异步客户端，供分片上传使用
     * @param minioProperties  MinIO 配置
     */
    public MinioUtil(MinioClient minioClient, MinioAsyncClient minioAsyncClient, MinioProperties minioProperties) {
        this(minioClient, new MinioMultipartClient(minioAsyncClient), minioProperties);
    }

    /**
     * 直接注入分片上传客户端，供同包单元测试替换成 mock
     *
     * @param minioClient     MinIO 客户端
     * @param multipartClient 分片上传客户端
     * @param minioProperties MinIO 配置
     */
    MinioUtil(MinioClient minioClient, MinioMultipartClient multipartClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.multipartClient = multipartClient;
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

    /**
     * 初始化默认桶中的分片上传，返回对象存储给出的分片上传 ID
     *
     * <p>用于「客户端切片上传」：客户端先拿到上传 ID，再把每一片交给服务端转发，
     * 最后合并成一个对象。整文件一次请求的 {@link #putObject} 不解决大文件的传输问题。
     *
     * @param objectName  对象名（含目录前缀）
     * @param contentType 文件类型，可以为空
     * @return 分片上传 ID
     */
    public String initMultipartUpload(String objectName, String contentType) {
        return initMultipartUpload(minioProperties.getBucket(), objectName, contentType);
    }

    /**
     * 初始化指定桶中的分片上传
     *
     * @param bucket      桶名
     * @param objectName  对象名
     * @param contentType 文件类型，可以为空
     * @return 分片上传 ID
     */
    public String initMultipartUpload(String bucket, String objectName, String contentType) {
        // Content-Type 只在初始化时写进对象元数据：合并后无法再改，缺失会退化成 application/octet-stream
        Multimap<String, String> headers = ArrayListMultimap.create();
        if (StringUtils.hasText(contentType)) {
            headers.put("Content-Type", contentType);
        }
        try {
            CreateMultipartUploadResponse response = multipartClient
                    .createMultipartUploadAsync(bucket, null, objectName, headers, null)
                    .get(MULTIPART_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return response.result().uploadId();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "初始化分片上传被中断：" + objectName, ex);
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "初始化分片上传失败：" + objectName, ex);
        }
    }

    /**
     * 上传一个分片到默认桶
     *
     * <p>同一个分片序号重复上传会覆盖之前的分片，天然可重试。
     *
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     * @param partNumber 分片序号，从 1 开始
     * @param data       分片内容
     * @return 分片 ETag，合并时由服务端从 listParts 重新获取，返回值仅供排查
     */
    public String uploadPart(String objectName, String uploadId, int partNumber, byte[] data) {
        return uploadPart(minioProperties.getBucket(), objectName, uploadId, partNumber, data);
    }

    /**
     * 上传一个分片到指定桶
     *
     * @param bucket     桶名
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     * @param partNumber 分片序号，从 1 开始
     * @param data       分片内容
     * @return 分片 ETag
     */
    public String uploadPart(String bucket, String objectName, String uploadId, int partNumber, byte[] data) {
        try {
            UploadPartResponse response = multipartClient
                    .uploadPartAsync(bucket, null, objectName, data, data.length, uploadId, partNumber, null, null)
                    .get(MULTIPART_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return response.etag();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR,
                    "上传分片被中断：" + objectName + "#" + partNumber, ex);
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR,
                    "上传分片失败：" + objectName + "#" + partNumber, ex);
        }
    }

    /**
     * 查询默认桶中某个分片上传已完成的分片序号
     *
     * <p>「哪些分片已经传完」以对象存储为准，不用缓存里的记录：缓存可能过期或写失败，
     * 而对象存储的分片状态才是合并时真正生效的那份。
     *
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     * @return 已上传的分片序号，按升序排列
     */
    public List<Integer> listUploadedPartNumbers(String objectName, String uploadId) {
        return listUploadedPartNumbers(minioProperties.getBucket(), objectName, uploadId);
    }

    /**
     * 查询指定桶中某个分片上传已完成的分片序号
     *
     * @param bucket     桶名
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     * @return 已上传的分片序号，按升序排列
     */
    public List<Integer> listUploadedPartNumbers(String bucket, String objectName, String uploadId) {
        return listParts(bucket, objectName, uploadId).stream()
                .map(Part::partNumber)
                .sorted()
                .toList();
    }

    /**
     * 查询默认桶中某个分片上传已完成的分片及大小
     *
     * <p>合并前用它校验「分片数量是否齐全、分片总大小是否等于声明的文件大小」，
     * 避免把不完整或长度不符的对象合并出来。
     *
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     * @return 分片序号到分片大小的映射，按序号升序
     */
    public Map<Integer, Long> listUploadedParts(String objectName, String uploadId) {
        return listUploadedParts(minioProperties.getBucket(), objectName, uploadId);
    }

    /**
     * 查询指定桶中某个分片上传已完成的分片及大小
     *
     * @param bucket     桶名
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     * @return 分片序号到分片大小的映射，按序号升序
     */
    public Map<Integer, Long> listUploadedParts(String bucket, String objectName, String uploadId) {
        List<Part> parts = listParts(bucket, objectName, uploadId);
        parts.sort(Comparator.comparingInt(Part::partNumber));
        Map<Integer, Long> result = new LinkedHashMap<>(parts.size());
        for (Part part : parts) {
            result.put(part.partNumber(), part.partSize());
        }
        return result;
    }

    /**
     * 合并默认桶中某个分片上传的全部分片
     *
     * <p>分片清单由本方法自己从对象存储查询（而不是由调用方传入），合并时只认对象存储里
     * 真实存在的分片与 ETag，避免客户端伪造 ETag 或漏传分片。
     *
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     */
    public void completeMultipartUpload(String objectName, String uploadId) {
        completeMultipartUpload(minioProperties.getBucket(), objectName, uploadId);
    }

    /**
     * 合并指定桶中某个分片上传的全部分片
     *
     * @param bucket     桶名
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     */
    public void completeMultipartUpload(String bucket, String objectName, String uploadId) {
        List<Part> parts = listParts(bucket, objectName, uploadId);
        if (parts.isEmpty()) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "没有已上传的分片，无法合并：" + objectName);
        }
        // 分片必须按序号升序提交，顺序不对合并出来的对象内容就是错的
        Part[] sortedParts = parts.stream()
                .sorted(Comparator.comparingInt(Part::partNumber))
                .toArray(Part[]::new);
        try {
            multipartClient
                    .completeMultipartUploadAsync(bucket, null, objectName, uploadId, sortedParts, null, null)
                    .get(MULTIPART_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "合并分片被中断：" + objectName, ex);
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "合并分片失败：" + objectName, ex);
        }
    }

    /**
     * 取消默认桶中某个分片上传，并清理已上传的分片
     *
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     */
    public void abortMultipartUpload(String objectName, String uploadId) {
        abortMultipartUpload(minioProperties.getBucket(), objectName, uploadId);
    }

    /**
     * 取消指定桶中某个分片上传，并清理已上传的分片
     *
     * @param bucket     桶名
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     */
    public void abortMultipartUpload(String bucket, String objectName, String uploadId) {
        try {
            multipartClient
                    .abortMultipartUploadAsync(bucket, null, objectName, uploadId, null, null)
                    .get(MULTIPART_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "取消分片上传被中断：" + objectName, ex);
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "取消分片上传失败：" + objectName, ex);
        }
    }

    /**
     * 查询某个分片上传的全部分片（自动翻页）
     *
     * @param bucket     桶名
     * @param objectName 对象名
     * @param uploadId   分片上传 ID
     * @return 已上传的分片，顺序不做保证
     */
    private List<Part> listParts(String bucket, String objectName, String uploadId) {
        List<Part> parts = new ArrayList<>();
        Integer partNumberMarker = null;
        try {
            while (true) {
                ListPartsResponse response = multipartClient
                        .listPartsAsync(bucket, null, objectName, LIST_PARTS_PAGE_SIZE, partNumberMarker, uploadId,
                                null, null)
                        .get(MULTIPART_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                List<Part> page = response.result().partList();
                if (page != null) {
                    parts.addAll(page);
                }
                if (!response.result().isTruncated()) {
                    return parts;
                }
                partNumberMarker = response.result().nextPartNumberMarker();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "查询分片被中断：" + objectName, ex);
        } catch (Exception ex) {
            throw new BizException(CommonErrorConstant.FILE_OPERATION_ERROR, "查询分片失败：" + objectName, ex);
        }
    }
}
