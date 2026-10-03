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

    /**
     * 物理删除用户的全部角色关联（含历史上被逻辑删除的残留行）
     *
     * <p>与 {@code infra_role_menu} 同理：{@code infra_user_role} 上有唯一键
     * {@code uk_infra_user_role_user_id_role_id(user_id, role_id)}，逻辑删除的行仍占着唯一键，
     * 覆盖授权后再分配同一个角色会报 {@code Duplicate entry}，所以覆盖前先物理删除。
     *
     * <p>注意：这里是手写 SQL，不受 {@code @TableLogic} 影响，必须写在
     * {@code resources/mapper/InfraUserRoleMapper.xml} 中。
     *
     * @param userId 用户 ID
     * @return 删除的行数
     */
    int deleteByUserId(@Param("userId") Long userId);
}
