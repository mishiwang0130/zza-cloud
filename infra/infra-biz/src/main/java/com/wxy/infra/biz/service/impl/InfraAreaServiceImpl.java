package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.constant.CommonConstant;
import com.wxy.common.core.util.TreeUtil;
import com.wxy.infra.biz.convert.InfraAreaConvert;
import com.wxy.infra.biz.mapper.InfraAreaMapper;
import com.wxy.infra.biz.po.InfraArea;
import com.wxy.infra.biz.service.InfraAreaService;
import com.wxy.infra.biz.vo.admin.AreaRespVO;
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
        Long effectiveParentId = parentId == null ? CommonConstant.ROOT_PARENT_ID : parentId;
        List<InfraArea> areas = infraAreaMapper.selectList(new LambdaQueryWrapper<InfraArea>()
                .eq(InfraArea::getParentId, effectiveParentId)
                .orderByAsc(InfraArea::getCode));
        return infraAreaConvert.toRespVOList(areas);
    }

    /**
     * 查询完整的省市区树
     *
     * @return 省级为根的树
     */
    @Override
    public List<AreaRespVO> listTree() {
        List<InfraArea> areas = infraAreaMapper.selectList(new LambdaQueryWrapper<InfraArea>()
                .orderByAsc(InfraArea::getCode));
        List<AreaRespVO> nodes = infraAreaConvert.toRespVOList(areas);
        return TreeUtil.build(nodes, AreaRespVO::getId, AreaRespVO::getParentId,
                AreaRespVO::setChildren, CommonConstant.ROOT_PARENT_ID);
    }
}
