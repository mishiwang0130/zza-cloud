package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraFileClient;
import com.wxy.infra.api.dto.FileRespDTO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 文件服务单元测试：批量换地址、存在性校验与远程异常口径。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalFileServiceImplTest {

    /** infra 文件接口 */
    @Mock
    private InfraFileClient infraFileClient;

    /** 被测服务 */
    private RentalFileServiceImpl fileService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        fileService = new RentalFileServiceImpl();
        ReflectionTestUtils.setField(fileService, "infraFileClient", infraFileClient);
    }

    /**
     * 重复 ID 与 null 先去重，再按 ID 换取地址
     */
    @Test
    @DisplayName("getFileUrlMap：去重后按 ID 换取预签名地址")
    void getFileUrlMapShouldReturnUrlMap() {
        when(infraFileClient.listByIds(List.of(5L)))
                .thenReturn(Result.success(List.of(new FileRespDTO(5L, "a.png", "admin/a.png", "http://minio/a"))));

        Map<Long, String> urlMap = fileService.getFileUrlMap(Arrays.asList(5L, 5L, null));

        assertThat(urlMap).containsEntry(5L, "http://minio/a");
    }

    /**
     * 空入参直接返回空映射，不调 infra
     */
    @Test
    @DisplayName("getFileUrlMap：入参为空时返回空映射")
    void getFileUrlMapShouldReturnEmptyForBlankInput() {
        assertThat(fileService.getFileUrlMap(null)).isEmpty();
        assertThat(fileService.getFileUrlMap(List.of())).isEmpty();
    }

    /**
     * 有文件查不到时报「图片文件不存在」
     */
    @Test
    @DisplayName("validateFilesExist：存在查不到的文件时按图片文件不存在报错")
    void validateFilesExistShouldRejectMissingFile() {
        when(infraFileClient.listByIds(List.of(5L, 6L)))
                .thenReturn(Result.success(List.of(new FileRespDTO(5L, "a.png", "admin/a.png", "http://minio/a"))));

        assertThatThrownBy(() -> fileService.validateFilesExist(List.of(5L, 6L)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.IMAGE_FILE_NOT_FOUND.code()));
    }

    /**
     * 调用本身异常（连接失败、序列化失败等）统一换成「依赖的基础服务调用失败」
     */
    @Test
    @DisplayName("getFileUrlMap：调用异常时换成 REMOTE_SERVICE_ERROR")
    void getFileUrlMapShouldWrapUnexpectedException() {
        when(infraFileClient.listByIds(List.of(5L))).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> fileService.getFileUrlMap(List.of(5L)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.REMOTE_SERVICE_ERROR.code()));
    }
}
