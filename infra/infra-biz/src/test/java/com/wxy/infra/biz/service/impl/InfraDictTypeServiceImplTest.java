package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraDictTypeConvert;
import com.wxy.infra.biz.mapper.InfraDictDataMapper;
import com.wxy.infra.biz.mapper.InfraDictTypeMapper;
import com.wxy.infra.biz.po.InfraDictType;
import com.wxy.infra.biz.vo.admin.DictTypeCreateReqVO;
import com.wxy.infra.biz.vo.admin.DictTypeUpdateReqVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 字典类型服务单元测试：编码唯一、删除保护与「改编码同步刷新字典数据」。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class InfraDictTypeServiceImplTest {

    /** 字典类型 Mapper */
    @Mock
    private InfraDictTypeMapper infraDictTypeMapper;

    /** 字典数据 Mapper */
    @Mock
    private InfraDictDataMapper infraDictDataMapper;

    /** 字典类型转换器 */
    @Mock
    private InfraDictTypeConvert infraDictTypeConvert;

    /** 被测服务 */
    private InfraDictTypeServiceImpl dictTypeService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        dictTypeService = new InfraDictTypeServiceImpl();
        ReflectionTestUtils.setField(dictTypeService, "infraDictTypeMapper", infraDictTypeMapper);
        ReflectionTestUtils.setField(dictTypeService, "infraDictDataMapper", infraDictDataMapper);
        ReflectionTestUtils.setField(dictTypeService, "infraDictTypeConvert", infraDictTypeConvert);
    }

    /**
     * 编码重复时不允许新增
     */
    @Test
    @DisplayName("createDictType：编码重复时报错")
    void createShouldRejectDuplicateType() {
        when(infraDictTypeMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> dictTypeService.createDictType(buildCreateReq("common_status")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.DICT_TYPE_CODE_EXISTS.code()));
        verify(infraDictTypeMapper, never()).insert(any(InfraDictType.class));
    }

    /**
     * 改编码要同步刷新字典数据，否则老数据会变成查不到的孤儿
     */
    @Test
    @DisplayName("updateDictType：改编码时同步刷新字典数据")
    void updateShouldSyncDictDataType() {
        when(infraDictTypeMapper.selectById(1L)).thenReturn(buildDictType(1L, "old_status"));
        when(infraDictTypeMapper.selectCount(any())).thenReturn(0L);
        DictTypeUpdateReqVO reqVO = new DictTypeUpdateReqVO();
        reqVO.setId(1L);
        reqVO.setName("通用状态");
        reqVO.setType("common_status");
        reqVO.setStatus(CommonStatusEnum.ENABLED.getValue());

        dictTypeService.updateDictType(reqVO);

        verify(infraDictTypeMapper).updateById(any(InfraDictType.class));
        verify(infraDictDataMapper).updateDictType("old_status", "common_status", 0L);
    }

    /**
     * 编码没变时不需要刷字典数据
     */
    @Test
    @DisplayName("updateDictType：编码未变时不刷字典数据")
    void updateShouldNotSyncWhenTypeUnchanged() {
        when(infraDictTypeMapper.selectById(1L)).thenReturn(buildDictType(1L, "common_status"));
        DictTypeUpdateReqVO reqVO = new DictTypeUpdateReqVO();
        reqVO.setId(1L);
        reqVO.setName("通用状态（改名）");
        reqVO.setType("common_status");
        reqVO.setStatus(CommonStatusEnum.ENABLED.getValue());

        dictTypeService.updateDictType(reqVO);

        verify(infraDictTypeMapper).updateById(any(InfraDictType.class));
        verify(infraDictDataMapper, never()).updateDictType(any(), any(), any());
    }

    /**
     * 类型下还有字典数据时不允许删除
     */
    @Test
    @DisplayName("deleteDictType：类型下有字典数据时报错")
    void deleteShouldRejectWhenDataExists() {
        when(infraDictTypeMapper.selectById(1L)).thenReturn(buildDictType(1L, "common_status"));
        when(infraDictDataMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> dictTypeService.deleteDictType(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.DICT_TYPE_IN_USE.code()));
        verify(infraDictTypeMapper, never()).deleteById(1L);
    }

    /**
     * 构造新增入参
     *
     * @param type 字典类型编码
     * @return 新增入参
     */
    private DictTypeCreateReqVO buildCreateReq(String type) {
        DictTypeCreateReqVO reqVO = new DictTypeCreateReqVO();
        reqVO.setName("通用状态");
        reqVO.setType(type);
        reqVO.setStatus(CommonStatusEnum.ENABLED.getValue());
        return reqVO;
    }

    /**
     * 构造字典类型实体
     *
     * @param id   类型 ID
     * @param type 类型编码
     * @return 字典类型实体
     */
    private InfraDictType buildDictType(Long id, String type) {
        InfraDictType po = new InfraDictType();
        po.setId(id);
        po.setName("通用状态");
        po.setType(type);
        po.setStatus(CommonStatusEnum.ENABLED.getValue());
        return po;
    }
}
