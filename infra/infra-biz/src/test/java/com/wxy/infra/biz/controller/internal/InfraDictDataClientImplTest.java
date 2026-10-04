package com.wxy.infra.biz.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.dto.DictDataSimpleDTO;
import com.wxy.infra.biz.convert.InfraDictDataConvert;
import com.wxy.infra.biz.service.InfraDictDataService;
import com.wxy.infra.biz.vo.admin.DictDataSimpleRespVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 字典读取服务间接口实现单元测试：复用管理端服务并转成服务间 DTO。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class InfraDictDataClientImplTest {

    /** 字典数据服务 */
    @Mock
    private InfraDictDataService infraDictDataService;

    /** 字典数据转换器 */
    @Mock
    private InfraDictDataConvert infraDictDataConvert;

    /** 被测实现 */
    private InfraDictDataClientImpl infraDictDataClientImpl;

    /**
     * 装配被测实现
     */
    @BeforeEach
    void setUp() {
        infraDictDataClientImpl = new InfraDictDataClientImpl();
        ReflectionTestUtils.setField(infraDictDataClientImpl, "infraDictDataService", infraDictDataService);
        ReflectionTestUtils.setField(infraDictDataClientImpl, "infraDictDataConvert", infraDictDataConvert);
    }

    /**
     * 按类型查询时把服务结果转成 DTO 返回
     */
    @Test
    @DisplayName("listByType：返回转换后的 DTO 列表")
    void listByTypeShouldReturnConvertedDto() {
        DictDataSimpleRespVO respVO = new DictDataSimpleRespVO("近地铁", "near_subway");
        when(infraDictDataService.listDictDataByType("rental_apartment_label")).thenReturn(List.of(respVO));
        when(infraDictDataConvert.toSimpleDTOList(List.of(respVO)))
                .thenReturn(List.of(new DictDataSimpleDTO("近地铁", "near_subway")));

        Result<List<DictDataSimpleDTO>> result = infraDictDataClientImpl.listByType("rental_apartment_label");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getLabel()).isEqualTo("近地铁");
        assertThat(result.getData().get(0).getValue()).isEqualTo("near_subway");
    }
}
