package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.common.storage.util.MinioUtil;
import com.wxy.infra.biz.bo.InfraFileChunkSessionBO;
import com.wxy.infra.biz.config.InfraFileProperties;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraFileConvert;
import com.wxy.infra.biz.enums.InfraFileChunkStatusEnum;
import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.mapper.InfraFileMapper;
import com.wxy.infra.biz.po.InfraFile;
import com.wxy.infra.biz.util.InfraRedisKeyUtil;
import com.wxy.infra.biz.vo.FileChunkInitReqVO;
import com.wxy.infra.biz.vo.FileChunkInitRespVO;
import com.wxy.infra.biz.vo.FileChunkUploadRespVO;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import com.wxy.infra.biz.vo.FileRespVO;
import com.wxy.infra.biz.vo.app.FileAppRespVO;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务单元测试：上传入参校验、对象名生成、未接对象存储时的降级，以及按 ID 批量查询。
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

    /** 文件转换器 */
    @Mock
    private InfraFileConvert infraFileConvert;

    /** Redis 工具 */
    @Mock
    private RedisUtil redisUtil;

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
        ReflectionTestUtils.setField(fileService, "infraFileConvert", infraFileConvert);
        ReflectionTestUtils.setField(fileService, "redisUtil", redisUtil);
        // 用真实配置对象：默认值就是 10MB 阈值 / 5MB 分片 / 200MB 上限 / 1 天会话
        ReflectionTestUtils.setField(fileService, "infraFileProperties", new InfraFileProperties());
        UserContextHolder.set(new LoginUser(1L, 1, "admin"));
    }

    /**
     * 清理线程上的登录用户：UserContextHolder 是 ThreadLocal，不清理会串到别的用例
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
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

    /**
     * 空入参直接返回空列表，不查库
     */
    @Test
    @DisplayName("listByIds：ids 为空时直接返回空列表，不查库")
    void listByIdsShouldReturnEmptyWhenIdsBlank() {
        assertThat(fileService.listByIds(null)).isEmpty();
        assertThat(fileService.listByIds(List.of())).isEmpty();

        verifyNoInteractions(infraFileMapper);
    }

    /**
     * 命中文件时回填 ID、对象名与预签名地址
     */
    @Test
    @DisplayName("listByIds：命中文件时回填预签名地址")
    void listByIdsShouldFillPresignedUrl() {
        when(infraFileMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(buildFile(9L, "客厅.png", "admin/20261004/room.png")));
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.presignedGetUrl("admin/20261004/room.png")).thenReturn("http://minio/room");

        // 重复 ID 与 null 都要被清洗掉：重复 ID 会重复签发地址，null 会直接把查询打挂
        List<FileRespVO> result = fileService.listByIds(Arrays.asList(9L, 9L, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(9L);
        assertThat(result.get(0).getName()).isEqualTo("客厅.png");
        assertThat(result.get(0).getPath()).isEqualTo("admin/20261004/room.png");
        assertThat(result.get(0).getUrl()).isEqualTo("http://minio/room");
    }

    /**
     * 未配置对象存储时给出明确错误码，而不是 NPE
     */
    @Test
    @DisplayName("listByIds：未配置对象存储时报文件操作失败")
    void listByIdsShouldFailWhenStorageNotConfigured() {
        when(infraFileMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(buildFile(9L, "a.png", "admin/20261004/a.png")));
        when(minioUtilProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> fileService.listByIds(List.of(9L)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorConstant.FILE_OPERATION_ERROR.code()));
    }

    /**
     * 用户端按 ID 查询只返回文件 ID 与访问地址
     */
    @Test
    @DisplayName("listAppByIds：只返回文件 ID 与访问地址")
    void listAppByIdsShouldReturnIdAndUrl() {
        when(infraFileMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(buildFile(9L, "客厅.png", "app/20261004/room.png")));
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.presignedGetUrl("app/20261004/room.png")).thenReturn("http://minio/room");
        when(infraFileConvert.toAppVOList(any())).thenReturn(List.of(new FileAppRespVO(9L, "http://minio/room")));

        List<FileAppRespVO> result = fileService.listAppByIds(List.of(9L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(9L);
        assertThat(result.get(0).getUrl()).isEqualTo("http://minio/room");
    }

    /**
     * 用户端按 ID 查询：入参为空时返回空列表，不查库
     */
    @Test
    @DisplayName("listAppByIds：入参为空时返回空列表")
    void listAppByIdsShouldReturnEmptyWhenIdsBlank() {
        assertThat(fileService.listAppByIds(null)).isEmpty();

        verifyNoInteractions(infraFileMapper);
    }

    /** 分片大小：与配置默认值保持一致 */
    private static final long CHUNK_SIZE = 5L * 1024 * 1024;

    /** 测试用的文件大小：12MB，按 5MiB 切片正好 3 片 */
    private static final long CHUNK_FILE_SIZE = 12L * 1024 * 1024;

    /**
     * 超过单文件上限时直接拒绝，不建会话也不碰缓存
     */
    @Test
    @DisplayName("initChunkUpload：超过单文件上限直接拒绝")
    void initChunkUploadShouldRejectOversizeFile() {
        FileChunkInitReqVO reqVO = buildInitReqVO(201L * 1024 * 1024);

        assertThatThrownBy(() -> fileService.initChunkUpload(reqVO, InfraFileSourceEnum.ADMIN))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.FILE_SIZE_EXCEEDED.code()));

        verifyNoInteractions(redisUtil);
    }

    /**
     * 新会话：返回分片大小与总分片数，并把会话写进缓存
     */
    @Test
    @DisplayName("initChunkUpload：新会话返回分片大小与总分片数")
    void initChunkUploadShouldCreateSession() {
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.initMultipartUpload(anyString(), anyString())).thenReturn("minio-upload-1");
        when(redisUtil.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);

        FileChunkInitRespVO respVO = fileService.initChunkUpload(buildInitReqVO(CHUNK_FILE_SIZE),
                InfraFileSourceEnum.ADMIN);

        assertThat(respVO.getUploadId()).isNotBlank();
        assertThat(respVO.getChunkSize()).isEqualTo(CHUNK_SIZE);
        assertThat(respVO.getTotalChunks()).isEqualTo(3);
        assertThat(respVO.getUploadedPartNumbers()).isEmpty();
        verify(redisUtil).set(eq(InfraRedisKeyUtil.fileChunkSessionKey(respVO.getUploadId())), any(), anyLong(),
                any());
    }

    /**
     * 命中续传索引：复用同一个会话并带回已上传的分片序号，不再新建分片上传
     */
    @Test
    @DisplayName("initChunkUpload：续传复用会话并返回已上传分片")
    void initChunkUploadShouldResumeSession() {
        InfraFileChunkSessionBO session = buildChunkSession(3, CHUNK_FILE_SIZE);
        when(redisUtil.get(InfraRedisKeyUtil.fileChunkResumeKey(1L, InfraFileSourceEnum.ADMIN, "md5-1",
                CHUNK_FILE_SIZE), String.class)).thenReturn("upload-1");
        when(redisUtil.get(InfraRedisKeyUtil.fileChunkSessionKey("upload-1"), InfraFileChunkSessionBO.class))
                .thenReturn(session);
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.listUploadedPartNumbers("admin/20261005/a.bin", "minio-upload-1")).thenReturn(List.of(1, 2));

        FileChunkInitRespVO respVO = fileService.initChunkUpload(buildInitReqVO(CHUNK_FILE_SIZE),
                InfraFileSourceEnum.ADMIN);

        assertThat(respVO.getUploadId()).isEqualTo("upload-1");
        assertThat(respVO.getUploadedPartNumbers()).containsExactly(1, 2);
        verify(minioUtil, never()).initMultipartUpload(anyString(), anyString());
    }

    /**
     * 分片序号越界时拒绝，避免把不存在的分片写进对象存储
     */
    @Test
    @DisplayName("uploadChunk：分片序号越界报错")
    void uploadChunkShouldRejectInvalidPartNumber() {
        stubChunkSession(buildChunkSession(3, CHUNK_FILE_SIZE));
        MultipartFile file = buildChunkFile(CHUNK_SIZE);

        assertThatThrownBy(() -> fileService.uploadChunk("upload-1", 4, file, InfraFileSourceEnum.ADMIN))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.FILE_CHUNK_NUMBER_INVALID.code()));
    }

    /**
     * 非末片小于 5MiB 时拒绝：否则合并阶段对象存储会报 EntityTooSmall
     */
    @Test
    @DisplayName("uploadChunk：非末片小于 5MiB 报错")
    void uploadChunkShouldRejectTooSmallPart() {
        stubChunkSession(buildChunkSession(3, CHUNK_FILE_SIZE));
        MultipartFile file = buildChunkFile(1024L);

        assertThatThrownBy(() -> fileService.uploadChunk("upload-1", 1, file, InfraFileSourceEnum.ADMIN))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.FILE_CHUNK_SIZE_INVALID.code()));
    }

    /**
     * 会话不存在或不属于当前用户时统一报会话过期
     */
    @Test
    @DisplayName("uploadChunk：会话不存在报会话过期")
    void uploadChunkShouldRejectUnknownSession() {
        MultipartFile file = buildChunkFile(CHUNK_SIZE);

        assertThatThrownBy(() -> fileService.uploadChunk("upload-404", 1, file, InfraFileSourceEnum.ADMIN))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode())
                                .isEqualTo(InfraErrorConstant.FILE_CHUNK_SESSION_NOT_FOUND.code()));
    }

    /**
     * 已合并的会话不再接受分片
     */
    @Test
    @DisplayName("uploadChunk：已合并的会话不再接受分片")
    void uploadChunkShouldRejectCompletedSession() {
        InfraFileChunkSessionBO session = buildChunkSession(3, CHUNK_FILE_SIZE);
        session.setStatus(InfraFileChunkStatusEnum.COMPLETED);
        session.setFileId(9L);
        stubChunkSession(session);
        MultipartFile file = buildChunkFile(CHUNK_SIZE);

        assertThatThrownBy(() -> fileService.uploadChunk("upload-1", 1, file, InfraFileSourceEnum.ADMIN))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode())
                                .isEqualTo(InfraErrorConstant.FILE_CHUNK_SESSION_NOT_FOUND.code()));
    }

    /**
     * 分片上传成功：返回 ETag 并给会话续期
     */
    @Test
    @DisplayName("uploadChunk：成功返回 ETag 并续期会话")
    void uploadChunkShouldReturnEtag() {
        stubChunkSession(buildChunkSession(3, CHUNK_FILE_SIZE));
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.uploadPart(anyString(), anyString(), anyInt(), any())).thenReturn("\"etag-1\"");
        MultipartFile file = buildChunkFile(CHUNK_SIZE);

        FileChunkUploadRespVO respVO = fileService.uploadChunk("upload-1", 1, file, InfraFileSourceEnum.ADMIN);

        assertThat(respVO.getPartNumber()).isEqualTo(1);
        assertThat(respVO.getEtag()).isEqualTo("\"etag-1\"");
        verify(redisUtil).set(eq(InfraRedisKeyUtil.fileChunkSessionKey("upload-1")), any(), anyLong(), any());
    }

    /**
     * 分片没传完不能合并：对象存储里只有部分分片时直接报错，不去合并
     */
    @Test
    @DisplayName("completeChunkUpload：分片不全报错")
    void completeChunkUploadShouldRejectIncompleteParts() {
        stubChunkSession(buildChunkSession(3, CHUNK_FILE_SIZE));
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.listUploadedParts(anyString(), anyString())).thenReturn(Map.of(1, CHUNK_SIZE));

        assertThatThrownBy(() -> fileService.completeChunkUpload("upload-1", InfraFileSourceEnum.ADMIN))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.FILE_CHUNK_INCOMPLETE.code()));
        verify(minioUtil, never()).completeMultipartUpload(anyString(), anyString());
    }

    /**
     * 已完成会话重复合并：直接返回缓存结果，不再调对象存储
     */
    @Test
    @DisplayName("completeChunkUpload：已完成会话返回缓存结果")
    void completeChunkUploadShouldReturnCachedResult() {
        InfraFileChunkSessionBO session = buildChunkSession(3, CHUNK_FILE_SIZE);
        session.setStatus(InfraFileChunkStatusEnum.COMPLETED);
        session.setFileId(9L);
        stubChunkSession(session);
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.presignedGetUrl("admin/20261005/a.bin")).thenReturn("http://minio/a");

        FileUploadRespVO respVO = fileService.completeChunkUpload("upload-1", InfraFileSourceEnum.ADMIN);

        assertThat(respVO.getFileId()).isEqualTo(9L);
        assertThat(respVO.getUrl()).isEqualTo("http://minio/a");
        verify(minioUtil, never()).listUploadedParts(anyString(), anyString());
        verify(minioUtil, never()).completeMultipartUpload(anyString(), anyString());
    }

    /**
     * 合并成功：落文件记录、标记会话已完成并释放续传索引
     */
    @Test
    @DisplayName("completeChunkUpload：合并后落库并释放续传索引")
    void completeChunkUploadShouldSaveRecord() {
        stubChunkSession(buildChunkSession(3, CHUNK_FILE_SIZE));
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.listUploadedParts(anyString(), anyString()))
                .thenReturn(Map.of(1, CHUNK_SIZE, 2, CHUNK_SIZE, 3, 2L * 1024 * 1024));
        when(minioUtil.presignedGetUrl("admin/20261005/a.bin")).thenReturn("http://minio/a");
        when(infraFileMapper.insert(any(InfraFile.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, InfraFile.class).setId(11L);
            return 1;
        });

        FileUploadRespVO respVO = fileService.completeChunkUpload("upload-1", InfraFileSourceEnum.ADMIN);

        assertThat(respVO.getFileId()).isEqualTo(11L);
        assertThat(respVO.getObjectName()).isEqualTo("admin/20261005/a.bin");
        verify(minioUtil).completeMultipartUpload("admin/20261005/a.bin", "minio-upload-1");
        verify(redisUtil).delete(InfraRedisKeyUtil.fileChunkResumeKey(1L, InfraFileSourceEnum.ADMIN, "md5-1",
                CHUNK_FILE_SIZE));
    }

    /**
     * 落库失败：补偿删除已合并的对象，并清掉会话与续传索引让客户端重传
     */
    @Test
    @DisplayName("completeChunkUpload：落库失败时补偿删除并清缓存")
    void completeChunkUploadShouldCleanUpWhenSaveRecordFails() {
        stubChunkSession(buildChunkSession(3, CHUNK_FILE_SIZE));
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);
        when(minioUtil.listUploadedParts(anyString(), anyString()))
                .thenReturn(Map.of(1, CHUNK_SIZE, 2, CHUNK_SIZE, 3, 2L * 1024 * 1024));
        when(infraFileMapper.insert(any(InfraFile.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> fileService.completeChunkUpload("upload-1", InfraFileSourceEnum.ADMIN))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.FILE_UPLOAD_ERROR.code()));

        verify(minioUtil).removeObject("admin/20261005/a.bin");
        verify(redisUtil).delete(InfraRedisKeyUtil.fileChunkSessionKey("upload-1"));
        verify(redisUtil).delete(InfraRedisKeyUtil.fileChunkResumeKey(1L, InfraFileSourceEnum.ADMIN, "md5-1",
                CHUNK_FILE_SIZE));
    }

    /**
     * 取消上传：取消对象存储的分片上传并清掉会话与续传索引
     */
    @Test
    @DisplayName("abortChunkUpload：取消分片上传并清缓存")
    void abortChunkUploadShouldAbortAndDeleteCache() {
        stubChunkSession(buildChunkSession(3, CHUNK_FILE_SIZE));
        when(minioUtilProvider.getIfAvailable()).thenReturn(minioUtil);

        fileService.abortChunkUpload("upload-1", InfraFileSourceEnum.ADMIN);

        verify(minioUtil).abortMultipartUpload("admin/20261005/a.bin", "minio-upload-1");
        verify(redisUtil).delete(InfraRedisKeyUtil.fileChunkSessionKey("upload-1"));
        verify(redisUtil).delete(InfraRedisKeyUtil.fileChunkResumeKey(1L, InfraFileSourceEnum.ADMIN, "md5-1",
                CHUNK_FILE_SIZE));
    }

    /**
     * 构造初始化入参
     *
     * @param fileSize 文件大小（字节）
     * @return 初始化入参
     */
    private FileChunkInitReqVO buildInitReqVO(long fileSize) {
        FileChunkInitReqVO reqVO = new FileChunkInitReqVO();
        reqVO.setFileName("a.bin");
        reqVO.setFileSize(fileSize);
        reqVO.setContentType("application/octet-stream");
        reqVO.setFileMd5("md5-1");
        return reqVO;
    }

    /**
     * 构造一个上传中的分片上传会话
     *
     * @param totalChunks 总分片数
     * @param fileSize    文件大小（字节）
     * @return 分片上传会话
     */
    private InfraFileChunkSessionBO buildChunkSession(int totalChunks, long fileSize) {
        InfraFileChunkSessionBO session = new InfraFileChunkSessionBO();
        session.setUploadId("upload-1");
        session.setMinioUploadId("minio-upload-1");
        session.setObjectName("admin/20261005/a.bin");
        session.setName("a.bin");
        session.setSize(fileSize);
        session.setContentType("application/octet-stream");
        session.setChunkSize(CHUNK_SIZE);
        session.setTotalChunks(totalChunks);
        session.setSource(InfraFileSourceEnum.ADMIN.name());
        session.setUserId(1L);
        session.setFileMd5("md5-1");
        session.setStatus(InfraFileChunkStatusEnum.UPLOADING);
        return session;
    }

    /**
     * 桩：按会话 ID 能查到会话
     *
     * @param session 会话
     */
    private void stubChunkSession(InfraFileChunkSessionBO session) {
        when(redisUtil.get(InfraRedisKeyUtil.fileChunkSessionKey(session.getUploadId()),
                InfraFileChunkSessionBO.class)).thenReturn(session);
    }

    /**
     * 构造分片文件
     *
     * @param size 分片大小（字节）
     * @return 分片文件
     */
    private MultipartFile buildChunkFile(long size) {
        return new MockMultipartFile("file", "part.bin", "application/octet-stream", new byte[(int) size]);
    }

    /**
     * 构造文件实体
     *
     * @param id   文件 ID
     * @param name 原始文件名
     * @param path 对象名
     * @return 文件实体
     */
    private InfraFile buildFile(Long id, String name, String path) {
        InfraFile file = new InfraFile();
        file.setId(id);
        file.setName(name);
        file.setPath(path);
        return file;
    }
}
