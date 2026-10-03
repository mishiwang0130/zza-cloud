package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.service.InfraPermissionService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * infra 权限校验实现单元测试：超管放行、权限集合匹配，以及端类型限制。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraPermissionCheckerTest {

    /** 权限服务 */
    @Mock
    private InfraPermissionService infraPermissionService;

    /** 被测实现 */
    private InfraPermissionChecker infraPermissionChecker;

    /**
     * 装配被测实现
     */
    @BeforeEach
    void setUp() {
        infraPermissionChecker = new InfraPermissionChecker();
        ReflectionTestUtils.setField(infraPermissionChecker, "infraPermissionService", infraPermissionService);
    }

    /**
     * 超级管理员直接放行，不查权限集合
     */
    @Test
    @DisplayName("hasAnyPermission：超级管理员直接放行")
    void shouldPassSuperAdmin() {
        when(infraPermissionService.isSuperAdmin(1L)).thenReturn(true);

        assertThat(infraPermissionChecker.hasAnyPermission(adminUser(1L),
                List.of(InfraPermissionConstant.USER_CREATE))).isTrue();
    }

    /**
     * 普通用户按权限集合判断
     */
    @Test
    @DisplayName("hasAnyPermission：命中权限集合时放行")
    void shouldPassWhenPermissionMatched() {
        when(infraPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(infraPermissionService.getPermissions(2L)).thenReturn(Set.of(InfraPermissionConstant.USER_CREATE));

        assertThat(infraPermissionChecker.hasAnyPermission(adminUser(2L),
                List.of(InfraPermissionConstant.USER_CREATE, InfraPermissionConstant.USER_DELETE))).isTrue();
    }

    /**
     * 权限集合没有所需标识时拒绝
     */
    @Test
    @DisplayName("hasAnyPermission：权限集合未命中时拒绝")
    void shouldRejectWhenPermissionMissing() {
        when(infraPermissionService.isSuperAdmin(3L)).thenReturn(false);
        when(infraPermissionService.getPermissions(3L)).thenReturn(Set.of(InfraPermissionConstant.USER_QUERY));

        assertThat(infraPermissionChecker.hasAnyPermission(adminUser(3L),
                List.of(InfraPermissionConstant.USER_DELETE))).isFalse();
    }

    /**
     * 管理后台的菜单权限不适用于 app 端身份
     */
    @Test
    @DisplayName("hasAnyPermission：非 admin 端身份一律拒绝")
    void shouldRejectNonAdminUserType() {
        LoginUser appUser = new LoginUser(4L, UserTypeEnum.APP.getValue(), "app-user");

        assertThat(infraPermissionChecker.hasAnyPermission(appUser,
                List.of(InfraPermissionConstant.USER_QUERY))).isFalse();
    }

    /**
     * 构造管理后台登录用户
     *
     * @param userId 用户 ID
     * @return 登录用户
     */
    private LoginUser adminUser(Long userId) {
        return new LoginUser(userId, UserTypeEnum.ADMIN.getValue(), "admin");
    }
}
