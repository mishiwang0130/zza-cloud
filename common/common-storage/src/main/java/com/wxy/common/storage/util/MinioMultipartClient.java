package com.wxy.common.storage.util;

import com.google.common.collect.Multimap;
import io.minio.AbortMultipartUploadResponse;
import io.minio.CreateMultipartUploadResponse;
import io.minio.ListPartsResponse;
import io.minio.MinioAsyncClient;
import io.minio.ObjectWriteResponse;
import io.minio.UploadPartResponse;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Part;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

/**
 * 分片上传客户端：把 {@link MinioAsyncClient} 里 protected 的分片 API 重新以 public 暴露出来。
 *
 * <p>minio 8.5.12 的 {@code S3Base} 把 createMultipartUpload / uploadPart / listParts /
 * completeMultipartUpload / abortMultipartUpload 全部声明为 protected（同步、异步都是），
 * 外部代码只能通过继承访问。这里用受保护的拷贝构造包一层，仅在本模块内部由 {@link MinioUtil}
 * 使用，不对外暴露；后续升级到这些方法已 public 的版本时，本类可以保留（等于重复声明可见性）
 * 或直接删除。
 *
 * @author wxy
 * @date 2026/10/05
 */
class MinioMultipartClient extends MinioAsyncClient {

    /**
     * 基于已构建好的异步客户端包装一层
     *
     * @param source 已构建好的 MinIO 异步客户端，与其共用同一套连接与凭据
     */
    MinioMultipartClient(MinioAsyncClient source) {
        super(source);
    }

    /**
     * 初始化分片上传
     *
     * @param bucketName       桶名
     * @param region           区域，传 null 表示由 SDK 自行解析
     * @param objectName       对象名
     * @param headers          请求头
     * @param extraQueryParams 额外查询参数
     * @return 初始化结果
     */
    @Override
    public CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadAsync(String bucketName, String region,
            String objectName, Multimap<String, String> headers, Multimap<String, String> extraQueryParams)
            throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
            NoSuchAlgorithmException, XmlParserException {
        return super.createMultipartUploadAsync(bucketName, region, objectName, headers, extraQueryParams);
    }

    /**
     * 上传分片
     *
     * @param bucketName       桶名
     * @param region           区域，传 null 表示由 SDK 自行解析
     * @param objectName       对象名
     * @param data             分片内容
     * @param length           分片长度
     * @param uploadId         分片上传 ID
     * @param partNumber       分片序号
     * @param extraHeaders     额外请求头
     * @param extraQueryParams 额外查询参数
     * @return 上传结果
     */
    @Override
    public CompletableFuture<UploadPartResponse> uploadPartAsync(String bucketName, String region, String objectName,
            Object data, long length, String uploadId, int partNumber, Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
            NoSuchAlgorithmException, XmlParserException {
        return super.uploadPartAsync(bucketName, region, objectName, data, length, uploadId, partNumber,
                extraHeaders, extraQueryParams);
    }

    /**
     * 查询已上传的分片
     *
     * @param bucketName         桶名
     * @param region             区域，传 null 表示由 SDK 自行解析
     * @param objectName         对象名
     * @param maxParts           单页最大分片数
     * @param partNumberMarker   分页起点，首屏传 null
     * @param uploadId           分片上传 ID
     * @param extraHeaders       额外请求头
     * @param extraQueryParams   额外查询参数
     * @return 查询结果
     */
    @Override
    public CompletableFuture<ListPartsResponse> listPartsAsync(String bucketName, String region, String objectName,
            Integer maxParts, Integer partNumberMarker, String uploadId, Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
            NoSuchAlgorithmException, XmlParserException {
        return super.listPartsAsync(bucketName, region, objectName, maxParts, partNumberMarker, uploadId,
                extraHeaders, extraQueryParams);
    }

    /**
     * 合并分片
     *
     * @param bucketName       桶名
     * @param region           区域，传 null 表示由 SDK 自行解析
     * @param objectName       对象名
     * @param uploadId         分片上传 ID
     * @param parts            分片列表
     * @param extraHeaders     额外请求头
     * @param extraQueryParams 额外查询参数
     * @return 合并结果
     */
    @Override
    public CompletableFuture<ObjectWriteResponse> completeMultipartUploadAsync(String bucketName, String region,
            String objectName, String uploadId, Part[] parts, Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
            NoSuchAlgorithmException, XmlParserException {
        return super.completeMultipartUploadAsync(bucketName, region, objectName, uploadId, parts, extraHeaders,
                extraQueryParams);
    }

    /**
     * 取消分片上传
     *
     * @param bucketName       桶名
     * @param region           区域，传 null 表示由 SDK 自行解析
     * @param objectName       对象名
     * @param uploadId         分片上传 ID
     * @param extraHeaders     额外请求头
     * @param extraQueryParams 额外查询参数
     * @return 取消结果
     */
    @Override
    public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUploadAsync(String bucketName, String region,
            String objectName, String uploadId, Multimap<String, String> extraHeaders,
            Multimap<String, String> extraQueryParams)
            throws InsufficientDataException, InternalException, InvalidKeyException, IOException,
            NoSuchAlgorithmException, XmlParserException {
        return super.abortMultipartUploadAsync(bucketName, region, objectName, uploadId, extraHeaders,
                extraQueryParams);
    }
}
