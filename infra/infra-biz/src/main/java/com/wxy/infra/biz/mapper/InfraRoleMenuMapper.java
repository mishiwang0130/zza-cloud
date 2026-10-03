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
}
