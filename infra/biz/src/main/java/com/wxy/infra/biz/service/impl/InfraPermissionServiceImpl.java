package com.wxy.infra.biz.service.impl;

import com.wxy.common.redis.util.RedisUtil;
import com.wxy.infra.biz.bo.InfraPermissionCacheBO;
import com.wxy.infra.biz.config.InfraTokenProperties;
import com.wxy.infra.biz.constant.InfraConstant;
import com.wxy.infra.biz.constant.InfraRedisKeyConstant;
import com.wxy.infra.biz.mapper.InfraMenuMapper;
import com.wxy.infra.biz.mapper.InfraRoleMapper;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.util.InfraRedisKeyUtil;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * 权限服务实现：以 MySQL 为权威，Redis 只做缓存。
 *
 * <p>缓存的是「角色编码 + 权限标识」一份数据，读一次缓存即可同时回答
 * 「是不是超管」与「有没有某个权限」，避免每个请求打两次库。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Service
public class InfraPermissionServiceImpl implements InfraPermissionService {

    /** Redis 读写工具 */
    @Resource
    private RedisUtil redisUtil;

    /** 角色 Mapper */
    @Resource
    private InfraRoleMapper infraRoleMapper;

    /** 菜单 Mapper */
    @Resource
    private InfraMenuMapper infraMenuMapper;

    /** 凭证与缓存配置 */
    @Resource
    private InfraTokenProperties infraTokenProperties;

    /**
     * 查询用户的权限标识集合
     *
     * @param userId 用户 ID
     * @return 权限标识集合，无权限时返回空集合
     */
    @Override
    public Set<String> getPermissions(Long userId) {
        return new LinkedHashSet<>(loadCache(userId).getPerms());
    }

    /**
     * 查询用户的角色编码集合
     *
     * @param userId 用户 ID
     * @return 角色编码集合，无角色时返回空集合
     */
    @Override
    public Set<String> getRoleCodes(Long userId) {
        return new LinkedHashSet<>(loadCache(userId).getRoleCodes());
    }

    /**
     * 判断用户是否为超级管理员
     *
     * @param userId 用户 ID，可以为 null
     * @return 是超管返回 true
     */
    @Override
    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return loadCache(userId).getRoleCodes().contains(InfraConstant.SUPER_ADMIN_ROLE_CODE);
    }

    /**
     * 清理单个用户的权限缓存
     *
     * @param userId 用户 ID，可以为 null（忽略）
     */
    @Override
    public void evictUser(Long userId) {
        if (userId == null) {
            return;
        }
        redisUtil.delete(InfraRedisKeyUtil.userPermissionKey(userId));
    }

    /**
     * 清理多个用户的权限缓存
     *
     * @param userIds 用户 ID 集合，可以为 null
     */
    @Override
    public void evictUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        userIds.forEach(this::evictUser);
    }

    /**
     * 清理全部用户的权限缓存
     */
    @Override
    public void evictAll() {
        Set<String> keys = redisUtil.scanKeys(InfraRedisKeyConstant.USER_PERMISSION + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        // 用 SCAN 拿到的 key 逐个删除，禁止使用 KEYS，避免阻塞 Redis
        keys.forEach(redisUtil::delete);
    }

    /**
     * 读取权限缓存，未命中则回查数据库并回写
     *
     * @param userId 用户 ID
     * @return 权限缓存对象
     */
    private InfraPermissionCacheBO loadCache(Long userId) {
        String key = InfraRedisKeyUtil.userPermissionKey(userId);
        InfraPermissionCacheBO cache = redisUtil.get(key, InfraPermissionCacheBO.class);
        if (cache != null) {
            return cache;
        }
        InfraPermissionCacheBO loaded = new InfraPermissionCacheBO();
        loaded.setRoleCodes(defaultList(infraRoleMapper.selectRoleCodesByUserId(userId)));
        loaded.setPerms(defaultList(infraMenuMapper.selectPermsByUserId(userId)));
        redisUtil.set(key, loaded, infraTokenProperties.getPermissionCacheSeconds(), TimeUnit.SECONDS);
        return loaded;
    }

    /**
     * 把可能为 null 的查询结果收敛成空列表，避免调用方到处判空
     *
     * @param values 查询结果，可以为 null
     * @return 非 null 列表
     */
    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
