package com.wxy.infra.biz.controller.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.dto.PermissionCheckReqDTO;
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
 * 权限服务间接口实现单元测试：超管放行、权限集合匹配、端类型限制。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraPermissionClientImplTest {

    /** 权限服务 */
    @Mock
    private InfraPermissionService infraPermissionService;

    /** 被测实现 */
    private InfraPermissionClientImpl infraPermissionClientImpl;

    /**
     * 装配被测实现
     */
    @BeforeEach
    void setUp() {
        infraPermissionClientImpl = new InfraPermissionClientImpl();
        ReflectionTestUtils.setField(infraPermissionClientImpl, "infraPermissionService", infraPermissionService);
    }

    /**
     * 超级管理员直接放行
     */
    @Test
    @DisplayName("hasAnyPermission：超级管理员直接放行")
    void shouldPassSuperAdmin() {
        when(infraPermissionService.isSuperAdmin(1L)).thenReturn(true);

        Result<Boolean> result = infraPermissionClientImpl.hasAnyPermission(
                buildReq(1L, UserTypeEnum.ADMIN.getValue(), InfraPermissionConstant.USER_CREATE));

        assertThat(result.getData()).isTrue();
    }

    /**
     * 普通用户按权限集合判断
     */
    @Test
    @DisplayName("hasAnyPermission：命中权限集合时返回 true")
    void shouldPassWhenPermissionMatched() {
        when(infraPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(infraPermissionService.getPermissions(2L)).thenReturn(Set.of(InfraPermissionConstant.USER_CREATE));

        Result<Boolean> result = infraPermissionClientImpl.hasAnyPermission(
                buildReq(2L, UserTypeEnum.ADMIN.getValue(), InfraPermissionConstant.USER_CREATE));

        assertThat(result.getData()).isTrue();
    }

    /**
     * 权限集合未命中时返回 false
     */
    @Test
    @DisplayName("hasAnyPermission：权限未命中时返回 false")
    void shouldRejectWhenPermissionMissing() {
        when(infraPermissionService.isSuperAdmin(3L)).thenReturn(false);
        when(infraPermissionService.getPermissions(3L)).thenReturn(Set.of(InfraPermissionConstant.USER_QUERY));

        Result<Boolean> result = infraPermissionClientImpl.hasAnyPermission(
                buildReq(3L, UserTypeEnum.ADMIN.getValue(), InfraPermissionConstant.USER_DELETE));

        assertThat(result.getData()).isFalse();
    }

    /**
     * 非 admin 端身份一律无权限
     */
    @Test
    @DisplayName("hasAnyPermission：非 admin 端身份返回 false")
    void shouldRejectNonAdminUserType() {
        when(infraPermissionService.isSuperAdmin(4L)).thenReturn(false);

        Result<Boolean> result = infraPermissionClientImpl.hasAnyPermission(
                buildReq(4L, UserTypeEnum.APP.getValue(), InfraPermissionConstant.USER_QUERY));

        assertThat(result.getData()).isFalse();
    }

    /**
     * 构造校验入参
     *
     * @param userId      用户 ID
     * @param userType    端类型
     * @param permission  权限标识
     * @return 入参
     */
    private PermissionCheckReqDTO buildReq(Long userId, Integer userType, String permission) {
        PermissionCheckReqDTO reqDTO = new PermissionCheckReqDTO();
        reqDTO.setUserId(userId);
        reqDTO.setUserType(userType);
        reqDTO.setPermissions(List.of(permission));
        return reqDTO;
    }
}
