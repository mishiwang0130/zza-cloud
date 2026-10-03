package com.wxy.infra.biz.service;

import java.util.Collection;
import java.util.Set;

/**
 * 权限服务：提供用户的角色编码与权限标识，并管理权限缓存。
 *
 * <p>权限数据以 MySQL 为权威：缓存未命中时回查「用户 → 角色 → 菜单」，
 * 授权关系变更时由调用方主动清理缓存，避免权限残留。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface InfraPermissionService {

    /**
     * 查询用户的权限标识集合
     *
     * @param userId 用户 ID
     * @return 权限标识集合，无权限时返回空集合
     */
    Set<String> getPermissions(Long userId);

    /**
     * 查询用户的角色编码集合
     *
     * @param userId 用户 ID
     * @return 角色编码集合，无角色时返回空集合
     */
    Set<String> getRoleCodes(Long userId);

    /**
     * 判断用户是否为超级管理员
     *
     * @param userId 用户 ID，可以为 null
     * @return 是超管返回 true
     */
    boolean isSuperAdmin(Long userId);

    /**
     * 清理单个用户的权限缓存
     *
     * @param userId 用户 ID，可以为 null（忽略）
     */
    void evictUser(Long userId);

    /**
     * 清理多个用户的权限缓存
     *
     * @param userIds 用户 ID 集合，可以为 null
     */
    void evictUsers(Collection<Long> userIds);

    /**
     * 清理全部用户的权限缓存
     *
     * <p>菜单这类可能影响任意用户的变更用它：逐个追查受影响用户不现实，
     * 而授权变更本身是低频管理操作，全量清理的代价可以接受。
     */
    void evictAll();
}
