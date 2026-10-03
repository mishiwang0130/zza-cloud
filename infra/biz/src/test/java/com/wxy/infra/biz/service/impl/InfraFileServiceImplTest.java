package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.storage.util.MinioUtil;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.vo.admin.FileUploadRespVO;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务单元测试：入参校验、对象名生成与未接对象存储时的降级。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraFileServiceImplTest {

    /** 对象存储工具提供者 */
    @Mock
    private ObjectProvider<MinioUtil> minioUtilProvider;

    /** 对象存储工具 */
    @Mock
    private MinioUtil minioUtil;

    /** 被测服务 */
    private InfraFileServiceImpl fileService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        fileService = new InfraFileServiceImpl();
        ReflectionTestUtils.setField(fileService, "minioUtilProvider", minioUtilProvider);
    }

    /**
     * 空文件直接拒绝，不发起存储调用
     */
    @Test
    @DisplayName("upload：空文件报「文件不能为空」")
    void uploadShouldRejectEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.FILE_EMPTY.code()));
    }

    /**
     * 未配置对象存储时给出明确提示，而不是抛出空指针
     */
    @Test
    @DisplayName("upload：未配置对象存储时报上传失败")
    void uploadShouldFailWhenStorageNotConfigured() {
        when(minioUtilProvider.getIfAvailable()).thenReturn(null);
        MultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.FILE_UPLOAD_ERROR.code()));
    }

    /**
     * 上传成功时对象名带 admin 与日期目录前缀，并返回预签名地址
     */
    @Test
    @DisplayName("upload：成功时生成 admin/{日期}/{uuid}.png 对象名并返回预签名地址")
    void uploadShouldReturnObjectNameAndUrl() {
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.presignedGetUrl(anyString())).thenReturn("http://minio/presigned");
        MultipartFile file = new MockMultipartFile("file", "头像.PNG", "image/png",
                "content".getBytes(StandardCharsets.UTF_8));

        FileUploadRespVO respVO = fileService.upload(file);

        assertThat(respVO.getObjectName()).startsWith("admin/").endsWith(".png");
        assertThat(respVO.getUrl()).isEqualTo("http://minio/presigned");
        verify(minioUtil).putObject(anyString(), any(), anyLong(), anyString());
    }
}
