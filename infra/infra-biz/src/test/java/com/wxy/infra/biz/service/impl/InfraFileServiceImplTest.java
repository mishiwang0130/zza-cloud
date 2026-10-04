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
import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.mapper.InfraFileMapper;
import com.wxy.infra.biz.po.InfraFile;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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

    /** 文件记录 Mapper */
    @Mock
    private InfraFileMapper infraFileMapper;

    /** 被测服务 */
    private InfraFileServiceImpl fileService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        fileService = new InfraFileServiceImpl();
        ReflectionTestUtils.setField(fileService, "minioUtilProvider", minioUtilProvider);
        ReflectionTestUtils.setField(fileService, "infraFileMapper", infraFileMapper);
    }

    /**
     * 空文件直接拒绝，不发起存储调用
     */
    @Test
    @DisplayName("upload：空文件报「文件不能为空」")
    void uploadShouldRejectEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileService.upload(file, InfraFileSourceEnum.ADMIN))
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

        assertThatThrownBy(() -> fileService.upload(file, InfraFileSourceEnum.ADMIN))
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

        FileUploadRespVO respVO = fileService.upload(file, InfraFileSourceEnum.ADMIN);

        assertThat(respVO.getObjectName()).startsWith("admin/").endsWith(".png");
        assertThat(respVO.getUrl()).isEqualTo("http://minio/presigned");
        verify(minioUtil).putObject(anyString(), any(), anyLong(), anyString());
    }

    /**
     * 上传成功时写入文件记录：原始文件名、对象名、大小、内容类型都要落库
     */
    @Test
    @DisplayName("upload：成功时写入文件记录")
    void uploadShouldSaveFileRecord() {
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.presignedGetUrl(anyString())).thenReturn("http://minio/presigned");
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", content);

        FileUploadRespVO respVO = fileService.upload(file, InfraFileSourceEnum.ADMIN);

        ArgumentCaptor<InfraFile> captor = ArgumentCaptor.forClass(InfraFile.class);
        verify(infraFileMapper).insert(captor.capture());
        InfraFile saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("report.pdf");
        assertThat(saved.getPath()).isEqualTo(respVO.getObjectName());
        assertThat(saved.getSize()).isEqualTo(content.length);
        assertThat(saved.getContentType()).isEqualTo("application/pdf");
    }

    /**
     * 入库失败时删除已上传的对象并报上传失败，避免留下孤儿文件
     */
    @Test
    @DisplayName("upload：文件记录入库失败时回滚对象并报错")
    void uploadShouldRollbackObjectWhenSaveRecordFails() {
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(infraFileMapper.insert(any(InfraFile.class))).thenThrow(new RuntimeException("db down"));
        MultipartFile file = new MockMultipartFile("file", "a.png", "image/png",
                "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> fileService.upload(file, InfraFileSourceEnum.ADMIN))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.FILE_UPLOAD_ERROR.code()));
        verify(minioUtil).removeObject(anyString());
    }

    /**
     * 上传成功时把落库生成的主键一起返回：调用方按 fileId 引用文件（app 头像、rental 图片都只存 ID）
     */
    @Test
    @DisplayName("upload：返回落库后生成的自增文件 ID")
    void uploadShouldReturnGeneratedFileId() {
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.presignedGetUrl(anyString())).thenReturn("http://minio/presigned");
        when(infraFileMapper.insert(any(InfraFile.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, InfraFile.class).setId(7L);
            return 1;
        });
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png",
                "content".getBytes(StandardCharsets.UTF_8));

        FileUploadRespVO respVO = fileService.upload(file, InfraFileSourceEnum.APP);

        assertThat(respVO.getFileId()).isEqualTo(7L);
        assertThat(respVO.getObjectName()).startsWith("app/").endsWith(".png");
    }
}
