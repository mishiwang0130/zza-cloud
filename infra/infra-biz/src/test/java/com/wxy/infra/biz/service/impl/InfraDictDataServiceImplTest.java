package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraDictDataConvert;
import com.wxy.infra.biz.mapper.InfraDictDataMapper;
import com.wxy.infra.biz.mapper.InfraDictTypeMapper;
import com.wxy.infra.biz.po.InfraDictData;
import com.wxy.infra.biz.vo.admin.DictDataCreateReqVO;
import com.wxy.infra.biz.vo.admin.DictDataSimpleRespVO;
import com.wxy.infra.biz.vo.app.DictDataAppRespVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 字典数据服务单元测试：类型存在性校验、类型内字典值唯一与按类型查询。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class InfraDictDataServiceImplTest {

    /** 字典数据 Mapper */
    @Mock
    private InfraDictDataMapper infraDictDataMapper;

    /** 字典类型 Mapper */
    @Mock
    private InfraDictTypeMapper infraDictTypeMapper;

    /** 字典数据转换器 */
    @Mock
    private InfraDictDataConvert infraDictDataConvert;

    /** 被测服务 */
    private InfraDictDataServiceImpl dictDataService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        dictDataService = new InfraDictDataServiceImpl();
        ReflectionTestUtils.setField(dictDataService, "infraDictDataMapper", infraDictDataMapper);
        ReflectionTestUtils.setField(dictDataService, "infraDictTypeMapper", infraDictTypeMapper);
        ReflectionTestUtils.setField(dictDataService, "infraDictDataConvert", infraDictDataConvert);
    }

    /**
     * 所属字典类型不存在时不允许新增，避免悬空数据
     */
    @Test
    @DisplayName("createDictData：字典类型不存在时报错")
    void createShouldRejectUnknownDictType() {
        when(infraDictTypeMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> dictDataService.createDictData(buildCreateReq("common_status", "0")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.DICT_TYPE_NOT_FOUND.code()));
    }

    /**
     * 同一类型下字典值重复时不允许新增
     */
    @Test
    @DisplayName("createDictData：同一类型下字典值重复时报错")
    void createShouldRejectDuplicateValue() {
        when(infraDictTypeMapper.selectCount(any())).thenReturn(1L);
        when(infraDictDataMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> dictDataService.createDictData(buildCreateReq("common_status", "0")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.DICT_DATA_VALUE_EXISTS.code()));
    }

    /**
     * 按类型查询返回精简结构，交给前端直接渲染下拉
     */
    @Test
    @DisplayName("listDictDataByType：返回标签与值")
    void listByTypeShouldReturnSimpleData() {
        InfraDictData po = new InfraDictData();
        po.setDictType("common_status");
        po.setLabel("启用");
        po.setValue("0");
        when(infraDictDataMapper.selectList(any())).thenReturn(List.of(po));
        when(infraDictDataConvert.toSimpleRespVOList(any()))
                .thenReturn(List.of(new DictDataSimpleRespVO("启用", "0")));

        List<DictDataSimpleRespVO> result = dictDataService.listDictDataByType("common_status");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLabel()).isEqualTo("启用");
        assertThat(result.get(0).getValue()).isEqualTo("0");
    }

    /**
     * 类型编码为空时直接返回空列表，不去查库
     */
    @Test
    @DisplayName("listDictDataByType：类型编码为空时返回空列表")
    void listByTypeShouldReturnEmptyWhenTypeBlank() {
        assertThat(dictDataService.listDictDataByType("  ")).isEmpty();
    }

    /**
     * 用户端按类型查询只返回标签与值，供匿名访客端渲染
     */
    @Test
    @DisplayName("listAppDictDataByType：返回标签与值")
    void listAppByTypeShouldReturnLabelAndValue() {
        InfraDictData po = new InfraDictData();
        po.setDictType("rental_room_orientation");
        po.setLabel("朝南");
        po.setValue("south");
        when(infraDictDataMapper.selectList(any())).thenReturn(List.of(po));
        when(infraDictDataConvert.toAppRespVOList(any()))
                .thenReturn(List.of(new DictDataAppRespVO("朝南", "south")));

        List<DictDataAppRespVO> result = dictDataService.listAppDictDataByType("rental_room_orientation");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLabel()).isEqualTo("朝南");
        assertThat(result.get(0).getValue()).isEqualTo("south");
    }

    /**
     * 用户端按类型查询：类型编码为空时直接返回空列表，不去查库
     */
    @Test
    @DisplayName("listAppDictDataByType：类型编码为空时返回空列表")
    void listAppByTypeShouldReturnEmptyWhenTypeBlank() {
        assertThat(dictDataService.listAppDictDataByType("")).isEmpty();
    }

    /**
     * 构造新增入参
     *
     * @param dictType 字典类型编码
     * @param value    字典值
     * @return 新增入参
     */
    private DictDataCreateReqVO buildCreateReq(String dictType, String value) {
        DictDataCreateReqVO reqVO = new DictDataCreateReqVO();
        reqVO.setDictType(dictType);
        reqVO.setLabel("启用");
        reqVO.setValue(value);
        reqVO.setSort(1);
        reqVO.setStatus(CommonStatusEnum.ENABLED.getValue());
        return reqVO;
    }
}
