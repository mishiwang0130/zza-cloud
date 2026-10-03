package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraMenuConvert;
import com.wxy.infra.biz.enums.InfraMenuTypeEnum;
import com.wxy.infra.biz.mapper.InfraMenuMapper;
import com.wxy.infra.biz.mapper.InfraRoleMenuMapper;
import com.wxy.infra.biz.po.InfraMenu;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.vo.admin.MenuRespVO;
import com.wxy.infra.biz.vo.admin.MenuUpdateReqVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 菜单服务单元测试：删除保护、上级校验与菜单树组装。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraMenuServiceImplTest {

    /** 菜单 Mapper */
    @Mock
    private InfraMenuMapper infraMenuMapper;

    /** 角色菜单关联 Mapper */
    @Mock
    private InfraRoleMenuMapper infraRoleMenuMapper;

    /** 菜单转换器 */
    @Mock
    private InfraMenuConvert infraMenuConvert;

    /** 权限服务 */
    @Mock
    private InfraPermissionService infraPermissionService;

    /** 被测服务 */
    private InfraMenuServiceImpl menuService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        menuService = new InfraMenuServiceImpl();
        ReflectionTestUtils.setField(menuService, "infraMenuMapper", infraMenuMapper);
        ReflectionTestUtils.setField(menuService, "infraRoleMenuMapper", infraRoleMenuMapper);
        ReflectionTestUtils.setField(menuService, "infraMenuConvert", infraMenuConvert);
        ReflectionTestUtils.setField(menuService, "infraPermissionService", infraPermissionService);
    }

    /**
     * 有子菜单时不允许删除，避免子节点变成孤儿
     */
    @Test
    @DisplayName("deleteMenu：存在子菜单时报错")
    void deleteMenuShouldRejectWhenChildrenExist() {
        when(infraMenuMapper.selectById(1L)).thenReturn(buildMenu(1L, 0L, InfraMenuTypeEnum.DIR));
        when(infraMenuMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> menuService.deleteMenu(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.MENU_HAS_CHILDREN.code()));
    }

    /**
     * 菜单被角色引用时不允许删除
     */
    @Test
    @DisplayName("deleteMenu：被角色引用时报错")
    void deleteMenuShouldRejectWhenAssignedToRole() {
        when(infraMenuMapper.selectById(2L)).thenReturn(buildMenu(2L, 1L, InfraMenuTypeEnum.MENU));
        // 先查子菜单（0 个），再查角色引用（1 个）
        when(infraMenuMapper.selectCount(any())).thenReturn(0L);
        when(infraRoleMenuMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> menuService.deleteMenu(2L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.MENU_IN_USE.code()));
    }

    /**
     * 删除成功后要清理权限缓存
     */
    @Test
    @DisplayName("deleteMenu：删除成功后清理权限缓存")
    void deleteMenuShouldEvictPermissionCache() {
        when(infraMenuMapper.selectById(3L)).thenReturn(buildMenu(3L, 1L, InfraMenuTypeEnum.BUTTON));
        when(infraMenuMapper.selectCount(any())).thenReturn(0L);
        when(infraRoleMenuMapper.selectCount(any())).thenReturn(0L);

        menuService.deleteMenu(3L);

        verify(infraMenuMapper).deleteById(3L);
        verify(infraPermissionService).evictAll();
    }

    /**
     * 不允许把自己设为上级菜单（否则形成环）
     */
    @Test
    @DisplayName("updateMenu：把自己设为上级时报错")
    void updateMenuShouldRejectSelfParent() {
        when(infraMenuMapper.selectById(1L)).thenReturn(buildMenu(1L, 0L, InfraMenuTypeEnum.DIR));
        MenuUpdateReqVO reqVO = new MenuUpdateReqVO();
        reqVO.setId(1L);
        reqVO.setParentId(1L);
        reqVO.setName("系统管理");
        reqVO.setType(InfraMenuTypeEnum.DIR.getValue());

        assertThatThrownBy(() -> menuService.updateMenu(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.MENU_PARENT_INVALID.code()));
    }

    /**
     * 菜单树要按 parentId 组装出层级
     */
    @Test
    @DisplayName("listMenuTree：按 parentId 组装出层级")
    void listMenuTreeShouldBuildHierarchy() {
        MenuRespVO root = buildNode(1L, 0L);
        MenuRespVO child = buildNode(2L, 1L);
        when(infraMenuMapper.selectList(any())).thenReturn(List.of(new InfraMenu()));
        when(infraMenuConvert.toRespVOList(any())).thenReturn(List.of(root, child));

        List<MenuRespVO> tree = menuService.listMenuTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getId()).isEqualTo(1L);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getId()).isEqualTo(2L);
    }

    /**
     * 构造菜单实体
     *
     * @param id       菜单 ID
     * @param parentId 上级菜单 ID
     * @param type     菜单类型
     * @return 菜单实体
     */
    private InfraMenu buildMenu(Long id, Long parentId, InfraMenuTypeEnum type) {
        InfraMenu menu = new InfraMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setName("菜单" + id);
        menu.setType(type.getValue());
        menu.setSort(1);
        return menu;
    }

    /**
     * 构造菜单节点
     *
     * @param id       菜单 ID
     * @param parentId 上级菜单 ID
     * @return 菜单节点
     */
    private MenuRespVO buildNode(Long id, Long parentId) {
        MenuRespVO node = new MenuRespVO();
        node.setId(id);
        node.setParentId(parentId);
        node.setName("菜单" + id);
        node.setType(InfraMenuTypeEnum.MENU.getValue());
        return node;
    }
}
