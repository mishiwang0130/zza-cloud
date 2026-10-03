package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色 Mapper：单表读写走 MyBatis-Plus，跨表查询写在 {@code resources/mapper/InfraRoleMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper
public interface InfraRoleMapper extends BaseMapper<InfraRole> {

    /**
     * 查询用户拥有的启用状态角色编码
     *
     * <p>只返回启用角色：停用角色不带任何权限，避免停用后权限仍然生效。
     *
     * @param userId 用户 ID
     * @return 角色编码列表，无角色时返回空列表
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
