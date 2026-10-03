package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraUserRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联 Mapper：单表读写走 MyBatis-Plus，按用户/角色反查写在 {@code resources/mapper/InfraUserRoleMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper
public interface InfraUserRoleMapper extends BaseMapper<InfraUserRole> {

    /**
     * 查询用户已分配的角色 ID
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表，未分配时返回空列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询拥有某个角色的用户 ID
     *
     * <p>角色授权变化时用它定位需要清理权限缓存的用户。
     *
     * @param roleId 角色 ID
     * @return 用户 ID 列表，无用户时返回空列表
     */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
