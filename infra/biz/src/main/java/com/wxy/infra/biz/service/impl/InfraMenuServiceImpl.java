package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.constant.CommonConstant;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.util.TreeUtil;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraMenuConvert;
import com.wxy.infra.biz.enums.InfraMenuTypeEnum;
import com.wxy.infra.biz.mapper.InfraMenuMapper;
import com.wxy.infra.biz.mapper.InfraRoleMenuMapper;
import com.wxy.infra.biz.po.InfraMenu;
import com.wxy.infra.biz.po.InfraRoleMenu;
import com.wxy.infra.biz.service.InfraMenuService;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.vo.admin.AuthMenuRespVO;
import com.wxy.infra.biz.vo.admin.MenuCreateReqVO;
import com.wxy.infra.biz.vo.admin.MenuRespVO;
import com.wxy.infra.biz.vo.admin.MenuUpdateReqVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 菜单服务实现：目录、菜单、按钮共用一张表，树形结构统一用 {@code TreeUtil} 组装。
 *
 * <p>删除做了两道保护：有子菜单不给删（否则子节点会变成孤儿），被角色引用不给删
 * （否则角色的权限会静默失效，排查起来很费劲）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Service
public class InfraMenuServiceImpl implements InfraMenuService {

    /** 向上追溯父节点时的最大层数：数据异常形成环时靠它跳出循环 */
    private static final int MAX_PARENT_DEPTH = 64;

    /** 菜单 Mapper */
    @Resource
    private InfraMenuMapper infraMenuMapper;

    /** 角色菜单关联 Mapper */
    @Resource
    private InfraRoleMenuMapper infraRoleMenuMapper;

    /** 菜单转换器 */
    @Resource
    private InfraMenuConvert infraMenuConvert;

    /** 权限服务：菜单变化后清理权限缓存 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /**
     * 新增菜单
     *
     * @param reqVO 新增入参
     * @return 新菜单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(MenuCreateReqVO reqVO) {
        validateMenuType(reqVO.getType());
        validateParent(reqVO.getParentId(), null);
        InfraMenu po = new InfraMenu();
        po.setParentId(reqVO.getParentId());
        po.setName(reqVO.getName());
        po.setType(reqVO.getType());
        po.setPath(defaultString(reqVO.getPath()));
        po.setComponent(defaultString(reqVO.getComponent()));
        po.setPerms(defaultString(reqVO.getPerms()));
        po.setIcon(defaultString(reqVO.getIcon()));
        po.setSort(reqVO.getSort() == null ? 0 : reqVO.getSort());
        po.setVisible(reqVO.getVisible() == null ? CommonStatusEnum.ENABLED.getValue() : reqVO.getVisible());
        po.setStatus(reqVO.getStatus() == null ? CommonStatusEnum.ENABLED.getValue() : reqVO.getStatus());
        infraMenuMapper.insert(po);
        return po.getId();
    }

    /**
     * 修改菜单
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(MenuUpdateReqVO reqVO) {
        validateMenuType(reqVO.getType());
        InfraMenu po = getExistingMenu(reqVO.getId());
        validateParent(reqVO.getParentId(), reqVO.getId());
        po.setParentId(reqVO.getParentId());
        po.setName(reqVO.getName());
        po.setType(reqVO.getType());
        po.setPath(defaultString(reqVO.getPath()));
        po.setComponent(defaultString(reqVO.getComponent()));
        po.setPerms(defaultString(reqVO.getPerms()));
        po.setIcon(defaultString(reqVO.getIcon()));
        if (reqVO.getSort() != null) {
            po.setSort(reqVO.getSort());
        }
        if (reqVO.getVisible() != null) {
            po.setVisible(reqVO.getVisible());
        }
        if (reqVO.getStatus() != null) {
            po.setStatus(reqVO.getStatus());
        }
        infraMenuMapper.updateById(po);
        // 权限标识可能变了，受影响用户无法逐个定位，直接全量清理权限缓存
        infraPermissionService.evictAll();
    }

    /**
     * 删除菜单（逻辑删除）
     *
     * @param id 菜单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        getExistingMenu(id);
        Long childCount = infraMenuMapper.selectCount(new LambdaQueryWrapper<InfraMenu>()
                .eq(InfraMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException(InfraErrorConstant.MENU_HAS_CHILDREN);
        }
        Long roleRefCount = infraRoleMenuMapper.selectCount(new LambdaQueryWrapper<InfraRoleMenu>()
                .eq(InfraRoleMenu::getMenuId, id));
        if (roleRefCount != null && roleRefCount > 0) {
            throw new BizException(InfraErrorConstant.MENU_IN_USE);
        }
        infraMenuMapper.deleteById(id);
        infraPermissionService.evictAll();
    }

    /**
     * 查询菜单详情
     *
     * @param id 菜单 ID
     * @return 菜单详情
     */
    @Override
    public MenuRespVO getMenu(Long id) {
        return infraMenuConvert.toRespVO(getExistingMenu(id));
    }

    /**
     * 查询全部菜单树（含按钮）
     *
     * @return 菜单树
     */
    @Override
    public List<MenuRespVO> listMenuTree() {
        List<InfraMenu> menus = infraMenuMapper.selectList(new LambdaQueryWrapper<InfraMenu>()
                .orderByAsc(InfraMenu::getSort)
                .orderByAsc(InfraMenu::getId));
        List<MenuRespVO> nodes = infraMenuConvert.toRespVOList(menus);
        return TreeUtil.build(nodes, MenuRespVO::getId, MenuRespVO::getParentId,
                MenuRespVO::setChildren, CommonConstant.ROOT_PARENT_ID);
    }

    /**
     * 查询指定用户的导航菜单树
     *
     * @param userId 用户 ID
     * @return 导航菜单树
     */
    @Override
    public List<AuthMenuRespVO> listMenuTreeByUser(Long userId) {
        List<InfraMenu> menus = infraMenuMapper.selectMenusByUserId(userId);
        List<AuthMenuRespVO> nodes = infraMenuConvert.toAuthMenuRespVOList(menus);
        return TreeUtil.build(nodes, AuthMenuRespVO::getId, AuthMenuRespVO::getParentId,
                AuthMenuRespVO::setChildren, CommonConstant.ROOT_PARENT_ID);
    }

    /**
     * 按 ID 查菜单，查不到直接报错
     *
     * @param id 菜单 ID
     * @return 菜单实体
     */
    private InfraMenu getExistingMenu(Long id) {
        InfraMenu po = id == null ? null : infraMenuMapper.selectById(id);
        if (po == null) {
            throw new BizException(InfraErrorConstant.MENU_NOT_FOUND);
        }
        return po;
    }

    /**
     * 校验菜单类型取值
     *
     * @param type 菜单类型
     */
    private void validateMenuType(Integer type) {
        if (InfraMenuTypeEnum.of(type) == null) {
            throw new BizException(CommonErrorConstant.PARAM_ERROR, "菜单类型只能是 1 目录、2 菜单、3 按钮");
        }
    }

    /**
     * 校验上级菜单：存在、不是自己、且不会形成环
     *
     * @param parentId 上级菜单 ID，0 表示顶级
     * @param selfId   当前菜单 ID，新增时传 null
     */
    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || CommonConstant.ROOT_PARENT_ID == parentId) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BizException(InfraErrorConstant.MENU_PARENT_INVALID);
        }
        InfraMenu parent = infraMenuMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException(InfraErrorConstant.MENU_PARENT_INVALID);
        }
        if (selfId == null) {
            return;
        }
        // 从上级往上追溯：一旦碰到自己，说明这次修改会把菜单挂到自己的子孙下面，形成环
        Long cursor = parent.getParentId();
        int depth = 0;
        while (cursor != null && cursor != CommonConstant.ROOT_PARENT_ID && depth < MAX_PARENT_DEPTH) {
            if (cursor.equals(selfId)) {
                throw new BizException(InfraErrorConstant.MENU_PARENT_INVALID);
            }
            InfraMenu ancestor = infraMenuMapper.selectById(cursor);
            if (ancestor == null) {
                break;
            }
            cursor = ancestor.getParentId();
            depth++;
        }
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
