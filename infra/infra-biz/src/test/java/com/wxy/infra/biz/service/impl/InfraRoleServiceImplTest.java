package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.infra.biz.convert.InfraRoleConvert;
import com.wxy.infra.biz.mapper.InfraMenuMapper;
import com.wxy.infra.biz.mapper.InfraRoleMapper;
import com.wxy.infra.biz.mapper.InfraRoleMenuMapper;
import com.wxy.infra.biz.mapper.InfraUserRoleMapper;
import com.wxy.infra.biz.po.InfraRole;
import com.wxy.infra.biz.po.InfraRoleMenu;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.vo.admin.RoleUpdateReqVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 角色服务单元测试：覆盖菜单权限必须「先物理删除旧关联、再写入新关联」。
 *
 * <p>{@code infra_role_menu} 上唯一键 {@code (role_id, menu_id)} 与逻辑删除互斥：
 * 逻辑删除只把 {@code is_delete} 置 1，行仍占着唯一键，若沿用 MyBatis-Plus 的逻辑删除，
 * 再次分配同一个菜单就会抛 {@code Duplicate entry '2-3'}。本测试锁住这个行为，防止回归。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraRoleServiceImplTest {

    /** 角色 Mapper */
    @Mock
    private InfraRoleMapper infraRoleMapper;

    /** 角色菜单关联 Mapper */
    @Mock
    private InfraRoleMenuMapper infraRoleMenuMapper;

    /** 用户角色关联 Mapper */
    @Mock
    private InfraUserRoleMapper infraUserRoleMapper;

    /** 菜单 Mapper */
    @Mock
    private InfraMenuMapper infraMenuMapper;

    /** 角色转换器 */
    @Mock
    private InfraRoleConvert infraRoleConvert;

    /** 权限服务 */
    @Mock
    private InfraPermissionService infraPermissionService;

    /** 被测服务 */
    private InfraRoleServiceImpl roleService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        roleService = new InfraRoleServiceImpl();
        ReflectionTestUtils.setField(roleService, "infraRoleMapper", infraRoleMapper);
        ReflectionTestUtils.setField(roleService, "infraRoleMenuMapper", infraRoleMenuMapper);
        ReflectionTestUtils.setField(roleService, "infraUserRoleMapper", infraUserRoleMapper);
        ReflectionTestUtils.setField(roleService, "infraMenuMapper", infraMenuMapper);
        ReflectionTestUtils.setField(roleService, "infraRoleConvert", infraRoleConvert);
        ReflectionTestUtils.setField(roleService, "infraPermissionService", infraPermissionService);
    }

    /**
     * 覆盖授权时先物理删除旧关联，避免唯一键冲突
     */
    @Test
    @DisplayName("updateRole：覆盖菜单权限时先物理删除旧关联，再整体写入")
    void updateRoleShouldPhysicallyDeleteRoleMenusBeforeInsert() {
        // 角色编码不变、菜单都存在，唯一性校验与菜单校验都通过
        when(infraRoleMapper.selectById(2L)).thenReturn(buildRole(2L, "ops"));
        when(infraRoleMapper.selectCount(any())).thenReturn(0L);
        when(infraMenuMapper.selectCount(any())).thenReturn(2L);
        when(infraUserRoleMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of());

        RoleUpdateReqVO reqVO = new RoleUpdateReqVO();
        reqVO.setId(2L);
        reqVO.setName("运维");
        reqVO.setCode("ops");
        reqVO.setStatus(CommonStatusEnum.ENABLED.getValue());
        // 其中 3 号菜单是历史上分配过又被移除的，逻辑删除会留下占着唯一键的残留行
        reqVO.setMenuIds(List.of(2L, 3L));

        roleService.updateRole(reqVO);

        // 物理删除必须发生在任何 insert 之前，否则残留行会让 insert 撞唯一键
        InOrder inOrder = inOrder(infraRoleMenuMapper);
        inOrder.verify(infraRoleMenuMapper).deleteByRoleId(2L);
        inOrder.verify(infraRoleMenuMapper, times(2)).insert(any(InfraRoleMenu.class));

        ArgumentCaptor<InfraRoleMenu> captor = ArgumentCaptor.forClass(InfraRoleMenu.class);
        verify(infraRoleMenuMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(InfraRoleMenu::getRoleId).containsOnly(2L);
        assertThat(captor.getAllValues()).extracting(InfraRoleMenu::getMenuId).containsExactly(2L, 3L);
    }

    /**
     * 不传 menuIds 表示不改权限，此时不应动关联表
     */
    @Test
    @DisplayName("updateRole：不传 menuIds 时不动菜单关联")
    void updateRoleShouldKeepRoleMenusWhenMenuIdsAbsent() {
        when(infraRoleMapper.selectById(2L)).thenReturn(buildRole(2L, "ops"));
        when(infraRoleMapper.selectCount(any())).thenReturn(0L);
        when(infraUserRoleMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of());

        RoleUpdateReqVO reqVO = new RoleUpdateReqVO();
        reqVO.setId(2L);
        reqVO.setName("运维");
        reqVO.setCode("ops");

        roleService.updateRole(reqVO);

        verify(infraRoleMenuMapper, times(0)).deleteByRoleId(2L);
        verify(infraRoleMenuMapper, times(0)).insert(any(InfraRoleMenu.class));
    }

    /**
     * 构造角色实体
     *
     * @param id   角色 ID
     * @param code 角色编码
     * @return 角色实体
     */
    private InfraRole buildRole(Long id, String code) {
        InfraRole role = new InfraRole();
        role.setId(id);
        role.setCode(code);
        role.setName("角色-" + id);
        role.setStatus(CommonStatusEnum.ENABLED.getValue());
        return role;
    }
}
