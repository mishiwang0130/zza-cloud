package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraDictTypeConvert;
import com.wxy.infra.biz.mapper.InfraDictDataMapper;
import com.wxy.infra.biz.mapper.InfraDictTypeMapper;
import com.wxy.infra.biz.po.InfraDictData;
import com.wxy.infra.biz.po.InfraDictType;
import com.wxy.infra.biz.service.InfraDictTypeService;
import com.wxy.infra.biz.vo.admin.DictTypeCreateReqVO;
import com.wxy.infra.biz.vo.admin.DictTypePageReqVO;
import com.wxy.infra.biz.vo.admin.DictTypeRespVO;
import com.wxy.infra.biz.vo.admin.DictTypeSimpleRespVO;
import com.wxy.infra.biz.vo.admin.DictTypeUpdateReqVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 字典类型服务实现。
 *
 * <p>两个业务约束：
 * <ol>
 *   <li>编码在服务内唯一：库里没加唯一索引（逻辑删除会占着唯一键，删了同编码再建会报 Duplicate entry），
 *       所以在这里按「未删除」的数据判断，重复时抛业务错误；</li>
 *   <li>改编码要同步刷新字典数据：字典数据按编码关联类型，不同步就会出现查不到的孤儿数据。</li>
 * </ol>
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class InfraDictTypeServiceImpl implements InfraDictTypeService {

    /** 字典类型 Mapper */
    @Resource
    private InfraDictTypeMapper infraDictTypeMapper;

    /** 字典数据 Mapper：删除保护与改编码同步都要用到 */
    @Resource
    private InfraDictDataMapper infraDictDataMapper;

    /** 字典类型转换器 */
    @Resource
    private InfraDictTypeConvert infraDictTypeConvert;

    /**
     * 新增字典类型
     *
     * @param reqVO 新增入参
     * @return 新字典类型 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDictType(DictTypeCreateReqVO reqVO) {
        if (countByType(reqVO.getType(), null) > 0) {
            throw new BizException(InfraErrorConstant.DICT_TYPE_CODE_EXISTS);
        }
        InfraDictType po = new InfraDictType();
        po.setName(reqVO.getName());
        po.setType(reqVO.getType());
        po.setStatus(reqVO.getStatus() == null ? CommonStatusEnum.ENABLED.getValue() : reqVO.getStatus());
        po.setRemark(defaultString(reqVO.getRemark()));
        infraDictTypeMapper.insert(po);
        return po.getId();
    }

    /**
     * 修改字典类型；改编码时同步刷新该类型下的字典数据
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictType(DictTypeUpdateReqVO reqVO) {
        InfraDictType po = getExistingDictType(reqVO.getId());
        boolean typeChanged = !po.getType().equals(reqVO.getType());
        if (typeChanged && countByType(reqVO.getType(), po.getId()) > 0) {
            throw new BizException(InfraErrorConstant.DICT_TYPE_CODE_EXISTS);
        }
        String oldType = po.getType();
        po.setName(reqVO.getName());
        po.setType(reqVO.getType());
        if (reqVO.getStatus() != null) {
            po.setStatus(reqVO.getStatus());
        }
        po.setRemark(defaultString(reqVO.getRemark()));
        infraDictTypeMapper.updateById(po);
        if (typeChanged) {
            // 数据行按编码关联类型，编码变了必须一起刷，否则这批数据就查不到了
            infraDictDataMapper.updateDictType(oldType, reqVO.getType(), UserContextHolder.getUserIdOrDefault());
        }
    }

    /**
     * 删除字典类型（逻辑删除）
     *
     * @param id 字典类型 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictType(Long id) {
        InfraDictType po = getExistingDictType(id);
        Long dataCount = infraDictDataMapper.selectCount(new LambdaQueryWrapper<InfraDictData>()
                .eq(InfraDictData::getDictType, po.getType()));
        if (dataCount != null && dataCount > 0) {
            // 直接删类型会让下面的数据查不到类型，属于悬空数据，先让使用者自己清理
            throw new BizException(InfraErrorConstant.DICT_TYPE_IN_USE);
        }
        infraDictTypeMapper.deleteById(id);
    }

    /**
     * 查询字典类型详情
     *
     * @param id 字典类型 ID
     * @return 字典类型详情
     */
    @Override
    public DictTypeRespVO getDictType(Long id) {
        return infraDictTypeConvert.toRespVO(getExistingDictType(id));
    }

    /**
     * 分页查询字典类型
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<DictTypeRespVO> pageDictType(DictTypePageReqVO reqVO) {
        Page<InfraDictType> page = PageUtil.toPage(reqVO);
        LambdaQueryWrapper<InfraDictType> wrapper = new LambdaQueryWrapper<InfraDictType>()
                .like(StringUtils.hasText(reqVO.getName()), InfraDictType::getName, reqVO.getName())
                .like(StringUtils.hasText(reqVO.getType()), InfraDictType::getType, reqVO.getType())
                .eq(reqVO.getStatus() != null, InfraDictType::getStatus, reqVO.getStatus())
                .orderByDesc(InfraDictType::getId);
        Page<InfraDictType> result = infraDictTypeMapper.selectPage(page, wrapper);
        return PageUtil.of(result, infraDictTypeConvert.toRespVOList(result.getRecords()));
    }

    /**
     * 查询启用状态的字典类型列表
     *
     * @return 字典类型精简列表
     */
    @Override
    public List<DictTypeSimpleRespVO> listDictType() {
        List<InfraDictType> types = infraDictTypeMapper.selectList(new LambdaQueryWrapper<InfraDictType>()
                .eq(InfraDictType::getStatus, CommonStatusEnum.ENABLED.getValue())
                .orderByAsc(InfraDictType::getId));
        return infraDictTypeConvert.toSimpleRespVOList(types);
    }

    /**
     * 按 ID 查字典类型，查不到直接报错
     *
     * @param id 字典类型 ID
     * @return 字典类型实体
     */
    private InfraDictType getExistingDictType(Long id) {
        InfraDictType po = id == null ? null : infraDictTypeMapper.selectById(id);
        if (po == null) {
            throw new BizException(InfraErrorConstant.DICT_TYPE_NOT_FOUND);
        }
        return po;
    }

    /**
     * 按编码统计字典类型数，排除自身
     *
     * @param type      字典类型编码
     * @param excludeId 需要排除的 ID，新增时传 null
     * @return 数量
     */
    private long countByType(String type, Long excludeId) {
        Long count = infraDictTypeMapper.selectCount(new LambdaQueryWrapper<InfraDictType>()
                .eq(InfraDictType::getType, type)
                .ne(excludeId != null, InfraDictType::getId, excludeId));
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
