package com.wxy.infra.biz.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.constant.CommonConstant;
import com.wxy.common.core.util.TreeUtil;
import com.wxy.infra.biz.convert.InfraAreaConvert;
import com.wxy.infra.biz.mapper.InfraAreaMapper;
import com.wxy.infra.biz.po.InfraArea;
import com.wxy.infra.biz.service.InfraAreaService;
import com.wxy.infra.biz.vo.admin.AreaRespVO;
import com.wxy.infra.biz.vo.app.AreaAppRespVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 行政区划服务实现：全部是只读查询。
 *
 * <p>不加 Redis 缓存：按上级查是一次走索引的窄查询，整棵树也只有 3000 多个节点，
 * 直查数据库比维护缓存更省心（数据是标准的，几乎不变，真要缓存也不难加）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class InfraAreaServiceImpl implements InfraAreaService {

    /** 区划 Mapper */
    @Resource
    private InfraAreaMapper infraAreaMapper;

    /** 区划转换器 */
    @Resource
    private InfraAreaConvert infraAreaConvert;

    /**
     * 查询某一级下的子级区划
     *
     * @param parentId 上级区划 ID；为 null 或 0 时返回全部省级
     * @return 子级区划列表
     */
    @Override
    public List<AreaRespVO> listChildren(Long parentId) {
        return infraAreaConvert.toRespVOList(listByParentId(parentId));
    }

    /**
     * 查询完整的省市区树
     *
     * @return 省级为根的树
     */
    @Override
    public List<AreaRespVO> listTree() {
        List<AreaRespVO> nodes = infraAreaConvert.toRespVOList(listAll());
        return TreeUtil.build(nodes, AreaRespVO::getId, AreaRespVO::getParentId,
                AreaRespVO::setChildren, CommonConstant.ROOT_PARENT_ID);
    }

    /**
     * 用户端查询某一级下的子级区划
     *
     * @param parentId 上级区划 ID；为 null 或 0 时返回全部省级
     * @return 子级区划列表（children 为空列表）
     */
    @Override
    public List<AreaAppRespVO> listAppChildren(Long parentId) {
        return infraAreaConvert.toAppRespVOList(listByParentId(parentId));
    }

    /**
     * 用户端查询完整的省市区树
     *
     * @return 省级为根的三级树
     */
    @Override
    public List<AreaAppRespVO> listAppTree() {
        List<AreaAppRespVO> nodes = infraAreaConvert.toAppRespVOList(listAll());
        return TreeUtil.build(nodes, AreaAppRespVO::getId, AreaAppRespVO::getParentId,
                AreaAppRespVO::setChildren, CommonConstant.ROOT_PARENT_ID);
    }

    @Override
    public List<AreaRespVO> listByCityName(String cityName) {
        if (StrUtil.isBlank(cityName)) {
            return null;
        }
        LambdaQueryWrapper<InfraArea> wrapper = new LambdaQueryWrapper<InfraArea>()
                .like(InfraArea::getName, cityName).last("limit 1");
        InfraArea infraArea = infraAreaMapper.selectOne(wrapper);
        if (infraArea == null) {
            return null;
        }
        return listChildren(infraArea.getId());
    }

    /**
     * 按上级 ID 查子级区划，排序按区划代码
     *
     * @param parentId 上级区划 ID；为 null 或 0 时返回全部省级
     * @return 区划实体列表
     */
    private List<InfraArea> listByParentId(Long parentId) {
        Long effectiveParentId = parentId == null ? CommonConstant.ROOT_PARENT_ID : parentId;
        return infraAreaMapper.selectList(new LambdaQueryWrapper<InfraArea>()
                .eq(InfraArea::getParentId, effectiveParentId)
                .orderByAsc(InfraArea::getCode));
    }

    /**
     * 查全部区划，按区划代码升序
     *
     * @return 区划实体列表
     */
    private List<InfraArea> listAll() {
        return infraAreaMapper.selectList(new LambdaQueryWrapper<InfraArea>()
                .orderByAsc(InfraArea::getCode));
    }
}
