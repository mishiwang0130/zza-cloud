package com.wxy.common.storage.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multimap;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.storage.config.MinioProperties;
import io.minio.AbortMultipartUploadResponse;
import io.minio.CreateMultipartUploadResponse;
import io.minio.ListPartsResponse;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.UploadPartResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MinIO 分片上传工具单元测试：验证参数转发、分片清单整理与异常包装。
 *
 * <p>分片上传客户端是包内可见的适配类，这里用同包测试替换成 mock，
 * 不依赖真实的 MinIO 服务。
 *
 * @author wxy
 * @date 2026/10/05
 */
@ExtendWith(MockitoExtension.class)
class MinioUtilTest {

    /** MinIO 同步客户端，本测试不涉及 */
    @Mock
    private MinioClient minioClient;

    /** 分片上传客户端 */
    @Mock
    private MinioMultipartClient multipartClient;

    /** 被测工具 */
    private MinioUtil minioUtil;

    /**
     * 装配被测工具：固定默认桶名为 zza
     */
    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties();
        properties.setBucket("zza");
        minioUtil = new MinioUtil(minioClient, multipartClient, properties);
    }

    /**
     * 初始化分片上传：写入 Content-Type 并返回对象存储给出的 uploadId
     */
    @Test
    @DisplayName("initMultipartUpload：带 Content-Type 并返回 uploadId")
    void initMultipartUploadShouldPassContentTypeAndReturnUploadId() throws Exception {
        InitiateMultipartUploadResult result = mock(InitiateMultipartUploadResult.class);
        when(result.uploadId()).thenReturn("minio-upload-1");
        CreateMultipartUploadResponse response = mock(CreateMultipartUploadResponse.class);
        when(response.result()).thenReturn(result);
        when(multipartClient.createMultipartUploadAsync(eq("zza"), isNull(), eq("admin/20261005/a.pdf"), any(),
                isNull())).thenReturn(CompletableFuture.completedFuture(response));

        String uploadId = minioUtil.initMultipartUpload("admin/20261005/a.pdf", "application/pdf");

        assertThat(uploadId).isEqualTo("minio-upload-1");
        ArgumentCaptor<Multimap<String, String>> captor = ArgumentCaptor.forClass(Multimap.class);
        verify(multipartClient).createMultipartUploadAsync(eq("zza"), isNull(), eq("admin/20261005/a.pdf"),
                captor.capture(), isNull());
        assertThat(captor.getValue().get("Content-Type")).containsExactly("application/pdf");
    }

    /**
     * 分片内容为空时不下发 Content-Type，避免写出一个空字符串的元数据
     */
    @Test
    @DisplayName("initMultipartUpload：内容类型为空时不写 Content-Type")
    void initMultipartUploadShouldSkipBlankContentType() throws Exception {
        CreateMultipartUploadResponse response = mock(CreateMultipartUploadResponse.class);
        when(response.result()).thenReturn(mock(InitiateMultipartUploadResult.class));
        when(multipartClient.createMultipartUploadAsync(eq("zza"), isNull(), eq("app/a"), any(), isNull()))
                .thenReturn(CompletableFuture.completedFuture(response));

        minioUtil.initMultipartUpload("app/a", null);

        ArgumentCaptor<Multimap<String, String>> captor = ArgumentCaptor.forClass(Multimap.class);
        verify(multipartClient).createMultipartUploadAsync(eq("zza"), isNull(), eq("app/a"), captor.capture(),
                isNull());
        assertThat(captor.getValue().get("Content-Type")).isEmpty();
    }

    /**
     * 上传分片：原样转发分片序号与长度，并返回 ETag
     */
    @Test
    @DisplayName("uploadPart：转发分片并返回 ETag")
    void uploadPartShouldReturnEtag() throws Exception {
        byte[] data = "chunk-content".getBytes(StandardCharsets.UTF_8);
        UploadPartResponse response = mock(UploadPartResponse.class);
        when(response.etag()).thenReturn("\"etag-2\"");
        when(multipartClient.uploadPartAsync(eq("zza"), isNull(), eq("admin/a.bin"), any(), eq((long) data.length),
                eq("u-1"), eq(2), isNull(), isNull())).thenReturn(CompletableFuture.completedFuture(response));

        String etag = minioUtil.uploadPart("admin/a.bin", "u-1", 2, data);

        assertThat(etag).isEqualTo("\"etag-2\"");
    }

    /**
     * 已上传分片序号：对象存储返回的顺序不做保证，工具要按升序整理
     */
    @Test
    @DisplayName("listUploadedPartNumbers：按分片序号升序返回")
    void listUploadedPartNumbersShouldSort() throws Exception {
        stubListParts(part(2), part(1));

        assertThat(minioUtil.listUploadedPartNumbers("admin/a.bin", "u-1")).containsExactly(1, 2);
    }

    /**
     * 已上传分片大小：返回分片序号到大小的映射，供合并前校验总大小
     */
    @Test
    @DisplayName("listUploadedParts：返回分片序号到大小的映射")
    void listUploadedPartsShouldReturnSizes() throws Exception {
        stubListParts(part(1, 5L), part(2, 4L));

        Map<Integer, Long> parts = minioUtil.listUploadedParts("admin/a.bin", "u-1");

        assertThat(parts).containsExactly(Map.entry(1, 5L), Map.entry(2, 4L));
    }

    /**
     * 合并分片：按分片序号升序提交，顺序错了合并出来的内容就是错的
     */
    @Test
    @DisplayName("completeMultipartUpload：按分片序号升序提交")
    void completeMultipartUploadShouldSortParts() throws Exception {
        stubListParts(part(3), part(1), part(2));
        when(multipartClient.completeMultipartUploadAsync(eq("zza"), isNull(), eq("admin/a.bin"), eq("u-1"), any(),
                isNull(), isNull())).thenReturn(CompletableFuture.completedFuture(mock(ObjectWriteResponse.class)));

        minioUtil.completeMultipartUpload("admin/a.bin", "u-1");

        ArgumentCaptor<Part[]> captor = ArgumentCaptor.forClass(Part[].class);
        verify(multipartClient).completeMultipartUploadAsync(eq("zza"), isNull(), eq("admin/a.bin"), eq("u-1"),
                captor.capture(), isNull(), isNull());
        assertThat(captor.getValue()).extracting(Part::partNumber).containsExactly(1, 2, 3);
    }

    /**
     * 没有任何分片时不能合并：直接报业务异常，不去调对象存储
     */
    @Test
    @DisplayName("completeMultipartUpload：没有分片时报文件操作失败")
    void completeMultipartUploadShouldFailWhenNoParts() throws Exception {
        stubListParts();

        assertThatThrownBy(() -> minioUtil.completeMultipartUpload("admin/a.bin", "u-1"))
                .isInstanceOf(BizException.class);
    }

    /**
     * 取消分片上传：转发到对象存储
     */
    @Test
    @DisplayName("abortMultipartUpload：转发取消请求")
    void abortMultipartUploadShouldForward() throws Exception {
        when(multipartClient.abortMultipartUploadAsync(eq("zza"), isNull(), eq("admin/a.bin"), eq("u-1"),
                isNull(), isNull()))
                .thenReturn(CompletableFuture.completedFuture(mock(AbortMultipartUploadResponse.class)));

        minioUtil.abortMultipartUpload("admin/a.bin", "u-1");

        verify(multipartClient).abortMultipartUploadAsync(eq("zza"), isNull(), eq("admin/a.bin"), eq("u-1"),
                isNull(), isNull());
    }

    /**
     * 对象存储报错时统一包装成 {@code BizException}，调用方不需要处理多重受检异常
     */
    @Test
    @DisplayName("initMultipartUpload：对象存储异常包装成 BizException")
    void initMultipartUploadShouldWrapException() throws Exception {
        when(multipartClient.createMultipartUploadAsync(eq("zza"), isNull(), eq("admin/a.bin"), any(), isNull()))
                .thenReturn(CompletableFuture.failedFuture(new IOException("minio down")));

        assertThatThrownBy(() -> minioUtil.initMultipartUpload("admin/a.bin", null))
                .isInstanceOf(BizException.class);
    }

    /**
     * 桩一个「单页、未截断」的分片查询结果
     *
     * @param parts 分片
     */
    private void stubListParts(Part... parts) throws Exception {
        ListPartsResult result = mock(ListPartsResult.class);
        when(result.partList()).thenReturn(List.of(parts));
        when(result.isTruncated()).thenReturn(false);
        ListPartsResponse response = mock(ListPartsResponse.class);
        when(response.result()).thenReturn(result);
        when(multipartClient.listPartsAsync(eq("zza"), isNull(), eq("admin/a.bin"), eq(1000), isNull(), eq("u-1"),
                isNull(), isNull())).thenReturn(CompletableFuture.completedFuture(response));
    }

    /**
     * 构造一个只有序号的分片
     *
     * @param partNumber 分片序号
     * @return 分片
     */
    private Part part(int partNumber) {
        Part part = mock(Part.class);
        when(part.partNumber()).thenReturn(partNumber);
        return part;
    }

    /**
     * 构造一个带序号与大小的分片
     *
     * @param partNumber 分片序号
     * @param partSize   分片大小
     * @return 分片
     */
    private Part part(int partNumber, long partSize) {
        Part part = mock(Part.class);
        when(part.partNumber()).thenReturn(partNumber);
        when(part.partSize()).thenReturn(partSize);
        return part;
    }
}
