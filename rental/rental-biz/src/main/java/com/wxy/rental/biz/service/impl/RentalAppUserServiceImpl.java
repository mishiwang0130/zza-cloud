package com.wxy.rental.biz.service.impl;

import com.wxy.common.core.util.RemoteCallUtil;
import com.wxy.infra.api.client.InfraAppUserClient;
import com.wxy.infra.api.dto.AppUserSimpleDTO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.service.RentalAppUserService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 用户档案服务实现：把批量查询的结果整成映射交给调用方。
 *
 * <p>刻意不缓存：用户随时可能改昵称，缓存住会让后台列表长期显示旧名字；
 * 一次列表只调一次 infra，代价可以接受。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Service
public class RentalAppUserServiceImpl implements RentalAppUserService {

    /** infra 用户端用户读取接口 */
    @Resource
    private InfraAppUserClient infraAppUserClient;

    /**
     * 按 ID 批量查询用户档案
     *
     * @param userIds 用户 ID 集合，可以为 null
     * @return 用户 ID 到用户档案的映射；查不到的用户不放进映射
     */
    @Override
    public Map<Long, AppUserSimpleDTO> getAppUserMap(Collection<Long> userIds) {
        List<Long> distinctIds = distinctUserIds(userIds);
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AppUserSimpleDTO> userMap = new LinkedHashMap<>();
        for (AppUserSimpleDTO user : listByIds(distinctIds)) {
            if (user != null && user.getId() != null) {
                userMap.put(user.getId(), user);
            }
        }
        return userMap;
    }

    /**
     * 调 infra 按 ID 批量查用户
     *
     * @param userIds 已去重的用户 ID 列表
     * @return 用户列表，infra 返回空时为空列表
     */
    private List<AppUserSimpleDTO> listByIds(List<Long> userIds) {
        List<AppUserSimpleDTO> users = RemoteCallUtil.call(
                () -> infraAppUserClient.listByIds(userIds).requireData(), "查询用户",
                RentalErrorConstant.REMOTE_SERVICE_ERROR);
        return users == null ? List.of() : users;
    }

    /**
     * 去重并过滤 null，避免同一次查询把重复 ID 交给 infra
     *
     * @param userIds 原始用户 ID 集合，可以为 null
     * @return 去重后的用户 ID 列表
     */
    private List<Long> distinctUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(userIds.stream().filter(Objects::nonNull).toList()));
    }
}
