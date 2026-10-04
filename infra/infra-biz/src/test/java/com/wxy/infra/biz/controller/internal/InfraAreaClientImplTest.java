package com.wxy.infra.biz.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.dto.AreaDTO;
import com.wxy.infra.biz.convert.InfraAreaConvert;
import com.wxy.infra.biz.service.InfraAreaService;
import com.wxy.infra.biz.vo.admin.AreaRespVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 行政区划服务间接口实现单元测试：子级查询与整棵树都复用管理端服务。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class InfraAreaClientImplTest {

    /** 行政区划服务 */
    @Mock
    private InfraAreaService infraAreaService;

    /** 行政区划转换器 */
    @Mock
    private InfraAreaConvert infraAreaConvert;

    /** 被测实现 */
    private InfraAreaClientImpl infraAreaClientImpl;

    /**
     * 装配被测实现
     */
    @BeforeEach
    void setUp() {
        infraAreaClientImpl = new InfraAreaClientImpl();
        ReflectionTestUtils.setField(infraAreaClientImpl, "infraAreaService", infraAreaService);
        ReflectionTestUtils.setField(infraAreaClientImpl, "infraAreaConvert", infraAreaConvert);
    }

    /**
     * 子级查询把服务结果转成 DTO 返回
     */
    @Test
    @DisplayName("listChildren：返回转换后的 DTO 列表")
    void listChildrenShouldReturnConvertedDto() {
        AreaRespVO node = new AreaRespVO();
        node.setId(3L);
        AreaDTO dto = new AreaDTO();
        dto.setId(3L);
        when(infraAreaService.listChildren(2L)).thenReturn(List.of(node));
        when(infraAreaConvert.toDTOList(List.of(node))).thenReturn(List.of(dto));

        Result<List<AreaDTO>> result = infraAreaClientImpl.listChildren(2L);

        assertThat(result.getData()).containsExactly(dto);
    }

    /**
     * 整棵树查询把服务结果转成 DTO 返回
     */
    @Test
    @DisplayName("listTree：返回转换后的 DTO 列表")
    void listTreeShouldReturnConvertedDto() {
        AreaRespVO root = new AreaRespVO();
        root.setId(1L);
        AreaDTO dto = new AreaDTO();
        dto.setId(1L);
        when(infraAreaService.listTree()).thenReturn(List.of(root));
        when(infraAreaConvert.toDTOList(List.of(root))).thenReturn(List.of(dto));

        Result<List<AreaDTO>> result = infraAreaClientImpl.listTree();

        assertThat(result.getData()).containsExactly(dto);
    }
}
