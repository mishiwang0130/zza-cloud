package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraRoleMenu;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色菜单关联 Mapper：单表读写走 MyBatis-Plus，按角色反查写在 {@code resources/mapper/InfraRoleMenuMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper
public interface InfraRoleMenuMapper extends BaseMapper<InfraRoleMenu> {

    /**
     * 查询角色已分配的菜单 ID
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表，未分配时返回空列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 物理删除角色的全部菜单关联（含历史上被逻辑删除的残留行）
     *
     * <p>关联表不保留历史：{@code infra_role_menu} 上有唯一键
     * {@code uk_infra_role_menu_role_id_menu_id(role_id, menu_id)}，而逻辑删除只把 {@code is_delete} 置 1，
     * 行还在表里；覆盖授权后再分配同一个菜单就会撞唯一键（{@code Duplicate entry '2-3'}）。
     * 因此覆盖授权前走这条物理删除，既清掉当前关联，也一并清掉之前逻辑删除留下的残留行。
     *
     * <p>注意：这里是手写 SQL，不受 {@code @TableLogic} 影响，必须写在
     * {@code resources/mapper/InfraRoleMenuMapper.xml} 中。
     *
     * @param roleId 角色 ID
     * @return 删除的行数
     */
    int deleteByRoleId(@Param("roleId") Long roleId);
}
