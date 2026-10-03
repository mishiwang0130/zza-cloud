package com.wxy.infra.biz.convert;

import com.wxy.infra.biz.po.InfraMenu;
import com.wxy.infra.biz.vo.admin.AuthMenuRespVO;
import com.wxy.infra.biz.vo.admin.MenuRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 菜单对象转换：实体到管理列表的树节点、以及当前用户的导航节点。
 *
 * <p>{@code children} 一律忽略：树结构由 Service 用 {@code TreeUtil} 组装，转换器只做平铺字段映射。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InfraMenuConvert {

    /**
     * 实体转管理端菜单节点
     *
     * @param po 菜单实体，可以为 null
     * @return 菜单节点，入参为 null 时返回 null
     */
    @Mapping(target = "children", ignore = true)
    MenuRespVO toRespVO(InfraMenu po);

    /**
     * 实体列表转管理端菜单节点列表
     *
     * @param list 菜单实体列表，可以为 null
     * @return 菜单节点列表，入参为 null 时返回 null
     */
    List<MenuRespVO> toRespVOList(List<InfraMenu> list);

    /**
     * 实体转导航菜单节点
     *
     * @param po 菜单实体，可以为 null
     * @return 导航菜单节点，入参为 null 时返回 null
     */
    @Mapping(target = "children", ignore = true)
    AuthMenuRespVO toAuthMenuRespVO(InfraMenu po);

    /**
     * 实体列表转导航菜单节点列表
     *
     * @param list 菜单实体列表，可以为 null
     * @return 导航菜单节点列表，入参为 null 时返回 null
     */
    List<AuthMenuRespVO> toAuthMenuRespVOList(List<InfraMenu> list);
}
