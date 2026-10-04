package com.wxy.infra.biz.service.impl;

import com.wxy.infra.biz.convert.InfraAppUserConvert;
import com.wxy.infra.biz.mapper.InfraAppUserMapper;
import com.wxy.infra.biz.po.InfraAppUser;
import com.wxy.infra.biz.service.InfraAppUserService;
import com.wxy.infra.biz.vo.AppUserRespVO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 用户端用户服务实现：去重后按 ID 批量查库，再交给转换器剥掉内部字段。
 *
 * <p>刻意不做缓存：用户随时可能改昵称，缓存住就会让后台列表长期显示旧名字，
 * 而这条查询只是按主键批量取几行，代价可以接受。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Service
public class InfraAppUserServiceImpl implements InfraAppUserService {

    /** 用户端用户 Mapper */
    @Resource
    private InfraAppUserMapper infraAppUserMapper;

    /** 用户端用户转换器 */
    @Resource
    private InfraAppUserConvert infraAppUserConvert;

    /**
     * 按 ID 批量查询用户端用户
     *
     * @param ids 用户 ID 列表，可以为 null
     * @return 用户列表，查不到的 ID 不返回
     */
    @Override
    public List<AppUserRespVO> listByIds(List<Long> ids) {
        List<Long> distinctIds = distinctIds(ids);
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        List<InfraAppUser> users = infraAppUserMapper.selectBatchIds(distinctIds);
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return infraAppUserConvert.toRespVOList(users);
    }

    /**
     * 去重并过滤 null，避免同一次查询把重复 ID 交给数据库
     *
     * @param ids 原始用户 ID 列表，可以为 null
     * @return 去重后的用户 ID 列表
     */
    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids.stream().filter(Objects::nonNull).toList()));
    }
}
