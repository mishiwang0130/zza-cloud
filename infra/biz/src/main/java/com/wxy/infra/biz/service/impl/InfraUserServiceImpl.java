package com.wxy.infra.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.ErrorCode;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.infra.biz.constant.InfraConstant;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraUserConvert;
import com.wxy.infra.biz.mapper.InfraRoleMapper;
import com.wxy.infra.biz.mapper.InfraUserMapper;
import com.wxy.infra.biz.mapper.InfraUserRoleMapper;
import com.wxy.infra.biz.po.InfraRole;
import com.wxy.infra.biz.po.InfraUser;
import com.wxy.infra.biz.po.InfraUserRole;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.service.InfraUserService;
import com.wxy.infra.biz.vo.admin.UserCreateReqVO;
import com.wxy.infra.biz.vo.admin.UserPageReqVO;
import com.wxy.infra.biz.vo.admin.UserResetPasswordReqVO;
import com.wxy.infra.biz.vo.admin.UserRespVO;
import com.wxy.infra.biz.vo.admin.UserUpdateReqVO;
import com.wxy.infra.biz.vo.admin.UserUpdateStatusReqVO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现：用户资料、状态、密码与角色分配。
 *
 * <p>三类保护贯穿所有写操作：不能删/停用自己，不能删/停用/重置超级管理员，
 * 不能通过接口把超级管理员角色分配给别人（否则任何有「用户修改」权限的人都能提权）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Service
public class InfraUserServiceImpl implements InfraUserService {

    /** 用户 Mapper */
    @Resource
    private InfraUserMapper infraUserMapper;

    /** 用户角色关联 Mapper */
    @Resource
    private InfraUserRoleMapper infraUserRoleMapper;

    /** 角色 Mapper */
    @Resource
    private InfraRoleMapper infraRoleMapper;

    /** 用户转换器 */
    @Resource
    private InfraUserConvert infraUserConvert;

    /** 权限服务：超管判定与权限缓存清理 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /** 密码编码器 */
    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 新增用户并分配角色
     *
     * @param reqVO 新增入参
     * @return 新用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateReqVO reqVO) {
        if (countByUsername(reqVO.getUsername()) > 0) {
            throw new BizException(InfraErrorConstant.USERNAME_EXISTS);
        }
        if (countByMobile(reqVO.getMobile(), null) > 0) {
            throw new BizException(InfraErrorConstant.MOBILE_EXISTS);
        }
        List<Long> roleIds = normalizeIds(reqVO.getRoleIds());
        validateRoleIds(roleIds, false);

        InfraUser po = new InfraUser();
        po.setUsername(reqVO.getUsername());
        po.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        po.setNickname(reqVO.getNickname());
        po.setMobile(reqVO.getMobile());
        po.setStatus(reqVO.getStatus() == null ? CommonStatusEnum.ENABLED.getValue() : reqVO.getStatus());
        infraUserMapper.insert(po);
        saveUserRoles(po.getId(), roleIds);
        return po.getId();
    }

    /**
     * 修改用户资料、状态与角色
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateReqVO reqVO) {
        InfraUser po = getExistingUser(reqVO.getId());
        if (countByMobile(reqVO.getMobile(), po.getId()) > 0) {
            throw new BizException(InfraErrorConstant.MOBILE_EXISTS);
        }
        boolean superAdmin = infraPermissionService.isSuperAdmin(po.getId());
        List<Long> roleIds = reqVO.getRoleIds() == null ? null : normalizeIds(reqVO.getRoleIds());
        if (roleIds != null) {
            // 已经是超管的用户允许保留该角色，但不允许把超管角色新分配给别的用户
            validateRoleIds(roleIds, superAdmin);
        }
        boolean disabled = reqVO.getStatus() != null && CommonStatusEnum.DISABLED.getValue().equals(reqVO.getStatus());
        if (disabled) {
            assertNotSuperAdmin(superAdmin);
            assertNotSelf(po.getId(), InfraErrorConstant.USER_SELF_DISABLE_FORBIDDEN);
        }
        po.setNickname(reqVO.getNickname());
        po.setMobile(reqVO.getMobile());
        if (reqVO.getStatus() != null) {
            po.setStatus(reqVO.getStatus());
        }
        infraUserMapper.updateById(po);
        if (roleIds != null) {
            replaceUserRoles(po.getId(), roleIds);
        }
        infraPermissionService.evictUser(po.getId());
    }

    /**
     * 删除用户（逻辑删除）并清空其角色关联
     *
     * @param id 用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        InfraUser po = getExistingUser(id);
        assertNotSelf(po.getId(), InfraErrorConstant.USER_SELF_DELETE_FORBIDDEN);
        assertNotSuperAdmin(infraPermissionService.isSuperAdmin(po.getId()));
        infraUserMapper.deleteById(po.getId());
        infraUserRoleMapper.delete(new LambdaQueryWrapper<InfraUserRole>()
                .eq(InfraUserRole::getUserId, po.getId()));
        infraPermissionService.evictUser(po.getId());
    }

    /**
     * 查询用户详情（含已分配角色）
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @Override
    public UserRespVO getUser(Long id) {
        InfraUser po = getExistingUser(id);
        UserRespVO vo = infraUserConvert.toRespVO(po);
        vo.setRoleIds(infraUserRoleMapper.selectRoleIdsByUserId(po.getId()));
        return vo;
    }

    /**
     * 分页查询用户
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<UserRespVO> pageUser(UserPageReqVO reqVO) {
        Page<InfraUser> page = PageUtil.toPage(reqVO);
        LambdaQueryWrapper<InfraUser> wrapper = new LambdaQueryWrapper<InfraUser>()
                .like(StringUtils.hasText(reqVO.getUsername()), InfraUser::getUsername, reqVO.getUsername())
                .like(StringUtils.hasText(reqVO.getNickname()), InfraUser::getNickname, reqVO.getNickname())
                .likeRight(StringUtils.hasText(reqVO.getMobile()), InfraUser::getMobile, reqVO.getMobile())
                .eq(reqVO.getStatus() != null, InfraUser::getStatus, reqVO.getStatus())
                .orderByDesc(InfraUser::getId);
        Page<InfraUser> result = infraUserMapper.selectPage(page, wrapper);
        List<UserRespVO> records = infraUserConvert.toRespVOList(result.getRecords());
        fillRoleIds(records);
        return PageUtil.of(result, records);
    }

    /**
     * 修改用户状态
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(UserUpdateStatusReqVO reqVO) {
        InfraUser po = getExistingUser(reqVO.getId());
        if (CommonStatusEnum.DISABLED.getValue().equals(reqVO.getStatus())) {
            assertNotSuperAdmin(infraPermissionService.isSuperAdmin(po.getId()));
            assertNotSelf(po.getId(), InfraErrorConstant.USER_SELF_DISABLE_FORBIDDEN);
        }
        po.setStatus(reqVO.getStatus());
        infraUserMapper.updateById(po);
        infraPermissionService.evictUser(po.getId());
    }

    /**
     * 管理员重置他人密码
     *
     * @param reqVO 重置入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(UserResetPasswordReqVO reqVO) {
        InfraUser po = getExistingUser(reqVO.getId());
        assertNotSuperAdmin(infraPermissionService.isSuperAdmin(po.getId()));
        po.setPassword(passwordEncoder.encode(reqVO.getNewPassword()));
        infraUserMapper.updateById(po);
    }

    /**
     * 按 ID 查用户，查不到直接报错
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    private InfraUser getExistingUser(Long id) {
        InfraUser po = id == null ? null : infraUserMapper.selectById(id);
        if (po == null) {
            throw new BizException(InfraErrorConstant.USER_NOT_FOUND);
        }
        return po;
    }

    /**
     * 按用户名统计用户数
     *
     * @param username 用户名
     * @return 数量
     */
    private long countByUsername(String username) {
        Long count = infraUserMapper.selectCount(new LambdaQueryWrapper<InfraUser>()
                .eq(InfraUser::getUsername, username));
        return count == null ? 0L : count;
    }

    /**
     * 统计手机号占用情况，排除自身
     *
     * @param mobile    手机号
     * @param excludeId 需要排除的用户 ID，新增时传 null
     * @return 数量
     */
    private long countByMobile(String mobile, Long excludeId) {
        Long count = infraUserMapper.selectCount(new LambdaQueryWrapper<InfraUser>()
                .eq(InfraUser::getMobile, mobile)
                .ne(excludeId != null, InfraUser::getId, excludeId));
        return count == null ? 0L : count;
    }

    /**
     * 校验角色 ID 是否合法
     *
     * @param roleIds         角色 ID 列表
     * @param allowSuperAdmin 是否允许包含超级管理员角色（仅当目标用户本来就是超管）
     */
    private void validateRoleIds(List<Long> roleIds, boolean allowSuperAdmin) {
        if (roleIds.isEmpty()) {
            return;
        }
        List<InfraRole> roles = infraRoleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BizException(InfraErrorConstant.ROLE_NOT_FOUND);
        }
        if (allowSuperAdmin) {
            return;
        }
        for (InfraRole role : roles) {
            if (InfraConstant.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
                throw new BizException(InfraErrorConstant.SUPER_ADMIN_PROTECTED,
                        "超级管理员角色不允许通过接口分配");
            }
        }
    }

    /**
     * 覆盖用户的角色关联
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表
     */
    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        infraUserRoleMapper.delete(new LambdaQueryWrapper<InfraUserRole>()
                .eq(InfraUserRole::getUserId, userId));
        saveUserRoles(userId, roleIds);
    }

    /**
     * 批量保存用户角色关联
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表
     */
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            InfraUserRole relation = new InfraUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            infraUserRoleMapper.insert(relation);
        }
    }

    /**
     * 填充返回体的角色 ID 列表
     *
     * <p>一次查出当前页所有用户的角色关联再分组，避免逐行查询造成 N+1。
     *
     * @param records 用户返回体列表
     */
    private void fillRoleIds(List<UserRespVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(UserRespVO::getId).toList();
        List<InfraUserRole> relations = infraUserRoleMapper.selectList(new LambdaQueryWrapper<InfraUserRole>()
                .in(InfraUserRole::getUserId, userIds));
        Map<Long, List<Long>> roleIdMap = relations.stream().collect(Collectors.groupingBy(
                InfraUserRole::getUserId,
                Collectors.mapping(InfraUserRole::getRoleId, Collectors.toList())));
        records.forEach(vo -> vo.setRoleIds(roleIdMap.getOrDefault(vo.getId(), List.of())));
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

    /**
     * 断言目标不是超级管理员
     *
     * @param superAdmin 目标是否为超级管理员
     */
    private void assertNotSuperAdmin(boolean superAdmin) {
        if (superAdmin) {
            throw new BizException(InfraErrorConstant.SUPER_ADMIN_PROTECTED);
        }
    }

    /**
     * 断言目标不是当前登录用户
     *
     * @param userId     目标用户 ID
     * @param errorCode  不满足时抛出的错误码
     */
    private void assertNotSelf(Long userId, ErrorCode errorCode) {
        if (userId.equals(UserContextHolder.getUserId())) {
            throw new BizException(errorCode);
        }
    }
}
