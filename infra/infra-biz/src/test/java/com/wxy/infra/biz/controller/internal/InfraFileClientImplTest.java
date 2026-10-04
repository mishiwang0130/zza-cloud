package com.wxy.infra.biz.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.dto.FileRespDTO;
import com.wxy.infra.biz.convert.InfraFileConvert;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.vo.admin.FileRespVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 文件读取服务间接口实现单元测试。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class InfraFileClientImplTest {

    /** 文件服务 */
    @Mock
    private InfraFileService infraFileService;

    /** 文件转换器 */
    @Mock
    private InfraFileConvert infraFileConvert;

    /** 被测实现 */
    private InfraFileClientImpl infraFileClientImpl;

    /**
     * 装配被测实现
     */
    @BeforeEach
    void setUp() {
        infraFileClientImpl = new InfraFileClientImpl();
        ReflectionTestUtils.setField(infraFileClientImpl, "infraFileService", infraFileService);
        ReflectionTestUtils.setField(infraFileClientImpl, "infraFileConvert", infraFileConvert);
    }

    /**
     * 按 ID 批量查询把服务结果转成 DTO 返回
     */
    @Test
    @DisplayName("listByIds：返回转换后的 DTO 列表")
    void listByIdsShouldReturnConvertedDto() {
        FileRespVO vo = new FileRespVO();
        vo.setId(5L);
        FileRespDTO dto = new FileRespDTO(5L, "a.png", "admin/a.png", "http://minio/a");
        when(infraFileService.listByIds(List.of(5L))).thenReturn(List.of(vo));
        when(infraFileConvert.toDTOList(List.of(vo))).thenReturn(List.of(dto));

        Result<List<FileRespDTO>> result = infraFileClientImpl.listByIds(List.of(5L));

        assertThat(result.getData()).containsExactly(dto);
        assertThat(result.getData().get(0).getUrl()).isEqualTo("http://minio/a");
    }
}
