package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraMenu;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 菜单 Mapper：单表读写走 MyBatis-Plus，权限与菜单树相关的跨表查询写在 {@code resources/mapper/InfraMenuMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper
public interface InfraMenuMapper extends BaseMapper<InfraMenu> {

    /**
     * 查询用户拥有的权限标识集合（用户 → 角色 → 菜单 → perms）
     *
     * @param userId 用户 ID
     * @return 去重后的权限标识列表，无权限时返回空列表
     */
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户可见的目录与菜单（不含按钮），用于生成导航菜单树
     *
     * @param userId 用户 ID
     * @return 目录与菜单列表（已按 sort、id 排序），无权限时返回空列表
     */
    List<InfraMenu> selectMenusByUserId(@Param("userId") Long userId);
}
