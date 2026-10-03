package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraUserConvert;
import com.wxy.infra.biz.mapper.InfraRoleMapper;
import com.wxy.infra.biz.mapper.InfraUserMapper;
import com.wxy.infra.biz.mapper.InfraUserRoleMapper;
import com.wxy.infra.biz.po.InfraRole;
import com.wxy.infra.biz.po.InfraUser;
import com.wxy.infra.biz.po.InfraUserRole;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.vo.admin.UserCreateReqVO;
import com.wxy.infra.biz.vo.admin.UserResetPasswordReqVO;
import com.wxy.infra.biz.vo.admin.UserUpdateReqVO;
import com.wxy.infra.biz.vo.admin.UserUpdateStatusReqVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 用户服务单元测试：唯一性校验与「不能删自己、不能动超管」两类保护。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraUserServiceImplTest {

    /** 用户 Mapper */
    @Mock
    private InfraUserMapper infraUserMapper;

    /** 用户角色关联 Mapper */
    @Mock
    private InfraUserRoleMapper infraUserRoleMapper;

    /** 角色 Mapper */
    @Mock
    private InfraRoleMapper infraRoleMapper;

    /** 用户转换器 */
    @Mock
    private InfraUserConvert infraUserConvert;

    /** 权限服务 */
    @Mock
    private InfraPermissionService infraPermissionService;

    /** 被测服务 */
    private InfraUserServiceImpl userService;

    /** 真实密码编码器 */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        userService = new InfraUserServiceImpl();
        ReflectionTestUtils.setField(userService, "infraUserMapper", infraUserMapper);
        ReflectionTestUtils.setField(userService, "infraUserRoleMapper", infraUserRoleMapper);
        ReflectionTestUtils.setField(userService, "infraRoleMapper", infraRoleMapper);
        ReflectionTestUtils.setField(userService, "infraUserConvert", infraUserConvert);
        ReflectionTestUtils.setField(userService, "infraPermissionService", infraPermissionService);
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
    }

    /**
     * 清理线程上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 用户名重复时不允许新增
     */
    @Test
    @DisplayName("createUser：用户名重复时报错")
    void createUserShouldRejectDuplicateUsername() {
        when(infraUserMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.createUser(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.USERNAME_EXISTS.code()));
    }

    /**
     * 手机号重复时不允许新增
     */
    @Test
    @DisplayName("createUser：手机号重复时报错")
    void createUserShouldRejectDuplicateMobile() {
        // 先查用户名（通过），再查手机号（冲突）
        when(infraUserMapper.selectCount(any())).thenReturn(0L, 1L);

        assertThatThrownBy(() -> userService.createUser(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.MOBILE_EXISTS.code()));
    }

    /**
     * 不能删除当前登录用户
     */
    @Test
    @DisplayName("deleteUser：删除自己时报错")
    void deleteUserShouldRejectSelf() {
        UserContextHolder.set(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));
        when(infraUserMapper.selectById(1L)).thenReturn(buildUser(1L));

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode())
                                .isEqualTo(InfraErrorConstant.USER_SELF_DELETE_FORBIDDEN.code()));
    }

    /**
     * 超级管理员账号不允许被删除
     */
    @Test
    @DisplayName("deleteUser：超级管理员账号受保护")
    void deleteUserShouldRejectSuperAdmin() {
        UserContextHolder.set(new LoginUser(9L, UserTypeEnum.ADMIN.getValue(), "ops"));
        when(infraUserMapper.selectById(1L)).thenReturn(buildUser(1L));
        when(infraPermissionService.isSuperAdmin(1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.SUPER_ADMIN_PROTECTED.code()));
    }

    /**
     * 超级管理员账号不允许被重置密码（应由本人走改密流程）
     */
    @Test
    @DisplayName("resetPassword：超级管理员账号受保护")
    void resetPasswordShouldRejectSuperAdmin() {
        when(infraUserMapper.selectById(1L)).thenReturn(buildUser(1L));
        when(infraPermissionService.isSuperAdmin(1L)).thenReturn(true);
        UserResetPasswordReqVO reqVO = new UserResetPasswordReqVO();
        reqVO.setId(1L);
        reqVO.setNewPassword("new-password");

        assertThatThrownBy(() -> userService.resetPassword(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.SUPER_ADMIN_PROTECTED.code()));
    }

    /**
     * 不能停用当前登录用户
     */
    @Test
    @DisplayName("updateStatus：停用自己时报错")
    void updateStatusShouldRejectSelfDisable() {
        UserContextHolder.set(new LoginUser(2L, UserTypeEnum.ADMIN.getValue(), "infra"));
        when(infraUserMapper.selectById(2L)).thenReturn(buildUser(2L));
        when(infraPermissionService.isSuperAdmin(2L)).thenReturn(false);
        UserUpdateStatusReqVO reqVO = new UserUpdateStatusReqVO();
        reqVO.setId(2L);
        reqVO.setStatus(CommonStatusEnum.DISABLED.getValue());

        assertThatThrownBy(() -> userService.updateStatus(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode())
                                .isEqualTo(InfraErrorConstant.USER_SELF_DISABLE_FORBIDDEN.code()));
    }

    /**
     * 覆盖角色时必须先物理删除旧关联，否则逻辑删除的残留行会撞唯一键
     */
    @Test
    @DisplayName("updateUser：覆盖角色时先物理删除旧关联，再整体写入")
    void updateUserShouldPhysicallyDeleteUserRolesBeforeInsert() {
        when(infraUserMapper.selectById(2L)).thenReturn(buildUser(2L));
        when(infraUserMapper.selectCount(any())).thenReturn(0L);
        when(infraPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(infraRoleMapper.selectBatchIds(any())).thenReturn(List.of(buildRole(3L), buildRole(4L)));

        UserUpdateReqVO reqVO = new UserUpdateReqVO();
        reqVO.setId(2L);
        reqVO.setNickname("基础服务管理员");
        reqVO.setMobile("13900000002");
        reqVO.setRoleIds(List.of(3L, 4L));

        userService.updateUser(reqVO);

        InOrder inOrder = inOrder(infraUserRoleMapper);
        inOrder.verify(infraUserRoleMapper).deleteByUserId(2L);
        inOrder.verify(infraUserRoleMapper, times(2)).insert(any(InfraUserRole.class));
    }

    /**
     * 构造新增用户入参
     *
     * @return 新增入参
     */
    private UserCreateReqVO buildCreateReq() {
        UserCreateReqVO reqVO = new UserCreateReqVO();
        reqVO.setUsername("infra");
        reqVO.setPassword("123456");
        reqVO.setNickname("基础服务管理员");
        reqVO.setMobile("13900000000");
        reqVO.setStatus(CommonStatusEnum.ENABLED.getValue());
        return reqVO;
    }

    /**
     * 构造用户实体
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    private InfraUser buildUser(Long id) {
        InfraUser user = new InfraUser();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setNickname("用户" + id);
        user.setMobile("1380000000" + id);
        user.setStatus(CommonStatusEnum.ENABLED.getValue());
        return user;
    }

    /**
     * 构造角色实体
     *
     * @param id 角色 ID
     * @return 角色实体
     */
    private InfraRole buildRole(Long id) {
        InfraRole role = new InfraRole();
        role.setId(id);
        role.setCode("role-" + id);
        role.setName("角色-" + id);
        role.setStatus(CommonStatusEnum.ENABLED.getValue());
        return role;
    }
}
