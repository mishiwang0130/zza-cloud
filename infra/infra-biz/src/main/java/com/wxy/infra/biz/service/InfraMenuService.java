package com.wxy.infra.biz.service;

import com.wxy.infra.biz.vo.admin.AuthMenuRespVO;
import com.wxy.infra.biz.vo.admin.MenuCreateReqVO;
import com.wxy.infra.biz.vo.admin.MenuRespVO;
import com.wxy.infra.biz.vo.admin.MenuUpdateReqVO;
import java.util.List;

/**
 * 管理后台菜单服务。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface InfraMenuService {

    /**
     * 新增菜单
     *
     * @param reqVO 新增入参
     * @return 新菜单 ID
     */
    Long createMenu(MenuCreateReqVO reqVO);

    /**
     * 修改菜单
     *
     * @param reqVO 修改入参
     */
    void updateMenu(MenuUpdateReqVO reqVO);

    /**
     * 删除菜单（逻辑删除），有子菜单或被角色引用时不允许删除
     *
     * @param id 菜单 ID
     */
    void deleteMenu(Long id);

    /**
     * 查询菜单详情
     *
     * @param id 菜单 ID
     * @return 菜单详情
     */
    MenuRespVO getMenu(Long id);

    /**
     * 查询全部菜单树（管理端，含按钮）
     *
     * @return 菜单树
     */
    List<MenuRespVO> listMenuTree();

    /**
     * 查询指定用户的导航菜单树（只含目录与菜单）
     *
     * @param userId 用户 ID
     * @return 导航菜单树
     */
    List<AuthMenuRespVO> listMenuTreeByUser(Long userId);
}
