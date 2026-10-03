package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.infra.biz.constant.InfraConstant;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraRoleConvert;
import com.wxy.infra.biz.mapper.InfraMenuMapper;
import com.wxy.infra.biz.mapper.InfraRoleMapper;
import com.wxy.infra.biz.mapper.InfraRoleMenuMapper;
import com.wxy.infra.biz.mapper.InfraUserRoleMapper;
import com.wxy.infra.biz.po.InfraMenu;
import com.wxy.infra.biz.po.InfraRole;
import com.wxy.infra.biz.po.InfraRoleMenu;
import com.wxy.infra.biz.po.InfraUserRole;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.service.InfraRoleService;
import com.wxy.infra.biz.vo.admin.RoleCreateReqVO;
import com.wxy.infra.biz.vo.admin.RolePageReqVO;
import com.wxy.infra.biz.vo.admin.RoleRespVO;
import com.wxy.infra.biz.vo.admin.RoleSimpleRespVO;
import com.wxy.infra.biz.vo.admin.RoleUpdateReqVO;
import com.wxy.infra.biz.vo.admin.RoleUpdateStatusReqVO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 角色服务实现：角色的增删改查与菜单权限分配。
 *
 * <p>超级管理员角色受保护：不允许删除、不允许改编码、不允许停用；
 * 已分配给用户的角色不允许删除，避免用户的权限静默消失。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Service
public class InfraRoleServiceImpl implements InfraRoleService {

    /** 角色 Mapper */
    @Resource
    private InfraRoleMapper infraRoleMapper;

    /** 角色菜单关联 Mapper */
    @Resource
    private InfraRoleMenuMapper infraRoleMenuMapper;

    /** 用户角色关联 Mapper */
    @Resource
    private InfraUserRoleMapper infraUserRoleMapper;

    /** 菜单 Mapper */
    @Resource
    private InfraMenuMapper infraMenuMapper;

    /** 角色转换器 */
    @Resource
    private InfraRoleConvert infraRoleConvert;

    /** 权限服务：角色授权变化后清理权限缓存 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /**
     * 新增角色并分配菜单权限
     *
     * @param reqVO 新增入参
     * @return 新角色 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleCreateReqVO reqVO) {
        if (countByCode(reqVO.getCode(), null) > 0) {
            throw new BizException(InfraErrorConstant.ROLE_CODE_EXISTS);
        }
        List<Long> menuIds = normalizeIds(reqVO.getMenuIds());
        validateMenuIds(menuIds);

        InfraRole po = new InfraRole();
        po.setName(reqVO.getName());
        po.setCode(reqVO.getCode());
        po.setSort(reqVO.getSort() == null ? 0 : reqVO.getSort());
        po.setStatus(reqVO.getStatus() == null ? CommonStatusEnum.ENABLED.getValue() : reqVO.getStatus());
        infraRoleMapper.insert(po);
        saveRoleMenus(po.getId(), menuIds);
        return po.getId();
    }

    /**
     * 修改角色并覆盖菜单权限
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleUpdateReqVO reqVO) {
        InfraRole po = getExistingRole(reqVO.getId());
        boolean superAdmin = InfraConstant.SUPER_ADMIN_ROLE_CODE.equals(po.getCode());
        if (superAdmin && !InfraConstant.SUPER_ADMIN_ROLE_CODE.equals(reqVO.getCode())) {
            throw new BizException(InfraErrorConstant.SUPER_ADMIN_ROLE_PROTECTED, "超级管理员角色的编码不允许修改");
        }
        if (countByCode(reqVO.getCode(), po.getId()) > 0) {
            throw new BizException(InfraErrorConstant.ROLE_CODE_EXISTS);
        }
        boolean disabled = CommonStatusEnum.DISABLED.getValue().equals(reqVO.getStatus());
        if (superAdmin && disabled) {
            throw new BizException(InfraErrorConstant.SUPER_ADMIN_ROLE_PROTECTED, "超级管理员角色不允许停用");
        }
        List<Long> menuIds = reqVO.getMenuIds() == null ? null : normalizeIds(reqVO.getMenuIds());
        if (menuIds != null) {
            validateMenuIds(menuIds);
        }
        po.setName(reqVO.getName());
        po.setCode(reqVO.getCode());
        if (reqVO.getSort() != null) {
            po.setSort(reqVO.getSort());
        }
        if (reqVO.getStatus() != null) {
            po.setStatus(reqVO.getStatus());
        }
        infraRoleMapper.updateById(po);
        if (menuIds != null) {
            replaceRoleMenus(po.getId(), menuIds);
        }
        // 角色权限或状态变了，拥有该角色的用户权限缓存必须失效
        infraPermissionService.evictUsers(infraUserRoleMapper.selectUserIdsByRoleId(po.getId()));
    }

    /**
     * 删除角色（逻辑删除）
     *
     * @param id 角色 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        InfraRole po = getExistingRole(id);
        if (InfraConstant.SUPER_ADMIN_ROLE_CODE.equals(po.getCode())) {
            throw new BizException(InfraErrorConstant.SUPER_ADMIN_ROLE_PROTECTED);
        }
        Long userCount = infraUserRoleMapper.selectCount(new LambdaQueryWrapper<InfraUserRole>()
                .eq(InfraUserRole::getRoleId, po.getId()));
        if (userCount != null && userCount > 0) {
            throw new BizException(InfraErrorConstant.ROLE_IN_USE);
        }
        infraRoleMapper.deleteById(po.getId());
        infraRoleMenuMapper.delete(new LambdaQueryWrapper<InfraRoleMenu>()
                .eq(InfraRoleMenu::getRoleId, po.getId()));
    }

    /**
     * 查询角色详情（含已分配菜单）
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    @Override
    public RoleRespVO getRole(Long id) {
        InfraRole po = getExistingRole(id);
        RoleRespVO vo = infraRoleConvert.toRespVO(po);
        vo.setMenuIds(infraRoleMenuMapper.selectMenuIdsByRoleId(po.getId()));
        return vo;
    }

    /**
     * 分页查询角色
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<RoleRespVO> pageRole(RolePageReqVO reqVO) {
        Page<InfraRole> page = PageUtil.toPage(reqVO);
        LambdaQueryWrapper<InfraRole> wrapper = new LambdaQueryWrapper<InfraRole>()
                .like(StringUtils.hasText(reqVO.getName()), InfraRole::getName, reqVO.getName())
                .like(StringUtils.hasText(reqVO.getCode()), InfraRole::getCode, reqVO.getCode())
                .eq(reqVO.getStatus() != null, InfraRole::getStatus, reqVO.getStatus())
                .orderByAsc(InfraRole::getSort)
                .orderByAsc(InfraRole::getId);
        Page<InfraRole> result = infraRoleMapper.selectPage(page, wrapper);
        return PageUtil.of(result, infraRoleConvert.toRespVOList(result.getRecords()));
    }

    /**
     * 查询启用状态的角色列表（下拉用）
     *
     * @return 角色精简列表
     */
    @Override
    public List<RoleSimpleRespVO> listRole() {
        List<InfraRole> roles = infraRoleMapper.selectList(new LambdaQueryWrapper<InfraRole>()
                .eq(InfraRole::getStatus, CommonStatusEnum.ENABLED.getValue())
                .orderByAsc(InfraRole::getSort)
                .orderByAsc(InfraRole::getId));
        return infraRoleConvert.toSimpleRespVOList(roles);
    }

    /**
     * 修改角色状态
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(RoleUpdateStatusReqVO reqVO) {
        InfraRole po = getExistingRole(reqVO.getId());
        if (InfraConstant.SUPER_ADMIN_ROLE_CODE.equals(po.getCode())) {
            throw new BizException(InfraErrorConstant.SUPER_ADMIN_ROLE_PROTECTED);
        }
        po.setStatus(reqVO.getStatus());
        infraRoleMapper.updateById(po);
        infraPermissionService.evictUsers(infraUserRoleMapper.selectUserIdsByRoleId(po.getId()));
    }

    /**
     * 按 ID 查角色，查不到直接报错
     *
     * @param id 角色 ID
     * @return 角色实体
     */
    private InfraRole getExistingRole(Long id) {
        InfraRole po = id == null ? null : infraRoleMapper.selectById(id);
        if (po == null) {
            throw new BizException(InfraErrorConstant.ROLE_NOT_FOUND);
        }
        return po;
    }

    /**
     * 按编码统计角色数，排除自身
     *
     * @param code      角色编码
     * @param excludeId 需要排除的角色 ID，新增时传 null
     * @return 数量
     */
    private long countByCode(String code, Long excludeId) {
        Long count = infraRoleMapper.selectCount(new LambdaQueryWrapper<InfraRole>()
                .eq(InfraRole::getCode, code)
                .ne(excludeId != null, InfraRole::getId, excludeId));
        return count == null ? 0L : count;
    }

    /**
     * 校验菜单 ID 是否都存在
     *
     * @param menuIds 菜单 ID 列表
     */
    private void validateMenuIds(List<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        Long count = infraMenuMapper.selectCount(new LambdaQueryWrapper<InfraMenu>()
                .in(InfraMenu::getId, menuIds));
        if (count == null || count != menuIds.size()) {
            throw new BizException(InfraErrorConstant.MENU_NOT_FOUND);
        }
    }

    /**
     * 覆盖角色的菜单关联
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表
     */
    private void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        infraRoleMenuMapper.delete(new LambdaQueryWrapper<InfraRoleMenu>()
                .eq(InfraRoleMenu::getRoleId, roleId));
        saveRoleMenus(roleId, menuIds);
    }

    /**
     * 批量保存角色菜单关联
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表
     */
    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            InfraRoleMenu relation = new InfraRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            infraRoleMenuMapper.insert(relation);
        }
    }

    /**
     * 去重并过滤 null，避免脏入参写进关联表
     *
     * @param ids 原始 ID 列表，可以为 null
     * @return 清洗后的 ID 列表
     */
    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids.stream().filter(Objects::nonNull).toList()));
    }
}
