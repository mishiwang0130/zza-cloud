package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraDictDataConvert;
import com.wxy.infra.biz.mapper.InfraDictDataMapper;
import com.wxy.infra.biz.mapper.InfraDictTypeMapper;
import com.wxy.infra.biz.po.InfraDictData;
import com.wxy.infra.biz.po.InfraDictType;
import com.wxy.infra.biz.service.InfraDictDataService;
import com.wxy.infra.biz.vo.admin.DictDataCreateReqVO;
import com.wxy.infra.biz.vo.admin.DictDataPageReqVO;
import com.wxy.infra.biz.vo.admin.DictDataRespVO;
import com.wxy.infra.biz.vo.admin.DictDataSimpleRespVO;
import com.wxy.infra.biz.vo.admin.DictDataUpdateReqVO;
import com.wxy.infra.biz.vo.app.DictDataAppRespVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 字典数据服务实现。
 *
 * <p>两条校验：所属字典类型必须存在（否则数据悬空、谁也查不到）；同一类型下的字典值唯一。
 * 唯一性同样按「未删除」的数据判断（库里没加唯一索引的原因见 {@code InfraDictTypeServiceImpl}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class InfraDictDataServiceImpl implements InfraDictDataService {

    /** 字典数据 Mapper */
    @Resource
    private InfraDictDataMapper infraDictDataMapper;

    /** 字典类型 Mapper：校验所属类型是否存在 */
    @Resource
    private InfraDictTypeMapper infraDictTypeMapper;

    /** 字典数据转换器 */
    @Resource
    private InfraDictDataConvert infraDictDataConvert;

    /**
     * 新增字典数据
     *
     * @param reqVO 新增入参
     * @return 新字典数据 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDictData(DictDataCreateReqVO reqVO) {
        validateDictTypeExists(reqVO.getDictType());
        if (countByTypeAndValue(reqVO.getDictType(), reqVO.getValue(), null) > 0) {
            throw new BizException(InfraErrorConstant.DICT_DATA_VALUE_EXISTS);
        }
        InfraDictData po = new InfraDictData();
        po.setDictType(reqVO.getDictType());
        po.setLabel(reqVO.getLabel());
        po.setValue(reqVO.getValue());
        po.setSort(reqVO.getSort() == null ? 0 : reqVO.getSort());
        po.setStatus(reqVO.getStatus() == null ? CommonStatusEnum.ENABLED.getValue() : reqVO.getStatus());
        po.setRemark(defaultString(reqVO.getRemark()));
        infraDictDataMapper.insert(po);
        return po.getId();
    }

    /**
     * 修改字典数据
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictData(DictDataUpdateReqVO reqVO) {
        InfraDictData po = getExistingDictData(reqVO.getId());
        validateDictTypeExists(reqVO.getDictType());
        if (countByTypeAndValue(reqVO.getDictType(), reqVO.getValue(), po.getId()) > 0) {
            throw new BizException(InfraErrorConstant.DICT_DATA_VALUE_EXISTS);
        }
        po.setDictType(reqVO.getDictType());
        po.setLabel(reqVO.getLabel());
        po.setValue(reqVO.getValue());
        if (reqVO.getSort() != null) {
            po.setSort(reqVO.getSort());
        }
        if (reqVO.getStatus() != null) {
            po.setStatus(reqVO.getStatus());
        }
        po.setRemark(defaultString(reqVO.getRemark()));
        infraDictDataMapper.updateById(po);
    }

    /**
     * 删除字典数据（逻辑删除）
     *
     * @param id 字典数据 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictData(Long id) {
        getExistingDictData(id);
        infraDictDataMapper.deleteById(id);
    }

    /**
     * 查询字典数据详情
     *
     * @param id 字典数据 ID
     * @return 字典数据详情
     */
    @Override
    public DictDataRespVO getDictData(Long id) {
        return infraDictDataConvert.toRespVO(getExistingDictData(id));
    }

    /**
     * 分页查询字典数据
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<DictDataRespVO> pageDictData(DictDataPageReqVO reqVO) {
        Page<InfraDictData> page = PageUtil.toPage(reqVO);
        LambdaQueryWrapper<InfraDictData> wrapper = new LambdaQueryWrapper<InfraDictData>()
                .eq(StringUtils.hasText(reqVO.getDictType()), InfraDictData::getDictType, reqVO.getDictType())
                .like(StringUtils.hasText(reqVO.getLabel()), InfraDictData::getLabel, reqVO.getLabel())
                .eq(reqVO.getStatus() != null, InfraDictData::getStatus, reqVO.getStatus())
                .orderByAsc(InfraDictData::getSort)
                .orderByAsc(InfraDictData::getId);
        Page<InfraDictData> result = infraDictDataMapper.selectPage(page, wrapper);
        return PageUtil.of(result, infraDictDataConvert.toRespVOList(result.getRecords()));
    }

    /**
     * 按类型编码查询启用的字典数据
     *
     * @param dictType 字典类型编码
     * @return 字典数据精简列表
     */
    @Override
    public List<DictDataSimpleRespVO> listDictDataByType(String dictType) {
        return infraDictDataConvert.toSimpleRespVOList(listEnabledByType(dictType));
    }

    /**
     * 用户端按类型编码查询启用的字典数据（只返回标签与值）
     *
     * @param dictType 字典类型编码
     * @return 用户端字典数据列表
     */
    @Override
    public List<DictDataAppRespVO> listAppDictDataByType(String dictType) {
        return infraDictDataConvert.toAppRespVOList(listEnabledByType(dictType));
    }

    /**
     * 按类型编码查启用数据并按排序号升序，admin 端与 app 端共用
     *
     * @param dictType 字典类型编码
     * @return 字典数据实体列表，类型编码为空白时返回空列表
     */
    private List<InfraDictData> listEnabledByType(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return List.of();
        }
        return infraDictDataMapper.selectList(new LambdaQueryWrapper<InfraDictData>()
                .eq(InfraDictData::getDictType, dictType)
                .eq(InfraDictData::getStatus, CommonStatusEnum.ENABLED.getValue())
                .orderByAsc(InfraDictData::getSort)
                .orderByAsc(InfraDictData::getId));
    }

    /**
     * 按 ID 查字典数据，查不到直接报错
     *
     * @param id 字典数据 ID
     * @return 字典数据实体
     */
    private InfraDictData getExistingDictData(Long id) {
        InfraDictData po = id == null ? null : infraDictDataMapper.selectById(id);
        if (po == null) {
            throw new BizException(InfraErrorConstant.DICT_DATA_NOT_FOUND);
        }
        return po;
    }

    /**
     * 校验字典类型是否存在：数据必须挂在真实存在的类型下
     *
     * @param dictType 字典类型编码
     */
    private void validateDictTypeExists(String dictType) {
        Long count = infraDictTypeMapper.selectCount(new LambdaQueryWrapper<InfraDictType>()
                .eq(InfraDictType::getType, dictType));
        if (count == null || count == 0) {
            throw new BizException(InfraErrorConstant.DICT_TYPE_NOT_FOUND);
        }
    }

    /**
     * 统计同一类型下的字典值数量，排除自身
     *
     * @param dictType  字典类型编码
     * @param value     字典值
     * @param excludeId 需要排除的 ID，新增时传 null
     * @return 数量
     */
    private long countByTypeAndValue(String dictType, String value, Long excludeId) {
        Long count = infraDictDataMapper.selectCount(new LambdaQueryWrapper<InfraDictData>()
                .eq(InfraDictData::getDictType, dictType)
                .eq(InfraDictData::getValue, value)
                .ne(excludeId != null, InfraDictData::getId, excludeId));
        return count == null ? 0L : count;
    }

    /**
     * 把可空字符串收敛为空串，避免库里出现 null 与空串两种「没填」
     *
     * @param value 原值，可以为 null
     * @return 原值或空串
     */
    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
