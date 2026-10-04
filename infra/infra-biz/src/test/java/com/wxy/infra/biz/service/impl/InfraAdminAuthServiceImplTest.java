package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.infra.biz.constant.InfraConstant;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.mapper.InfraUserMapper;
import com.wxy.infra.biz.po.InfraUser;
import com.wxy.infra.biz.service.InfraMenuService;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.vo.admin.AuthLoginReqVO;
import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.admin.AuthUpdatePasswordReqVO;
import com.wxy.infra.biz.vo.admin.AuthUserInfoRespVO;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 管理后台认证服务单元测试：登录校验、当前用户信息与改密。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraAdminAuthServiceImplTest {

    /** 用户 Mapper */
    @Mock
    private InfraUserMapper infraUserMapper;

    /** 凭证服务 */
    @Mock
    private InfraTokenService infraTokenService;

    /** 权限服务 */
    @Mock
    private InfraPermissionService infraPermissionService;

    /** 菜单服务 */
    @Mock
    private InfraMenuService infraMenuService;

    /** 被测服务 */
    private InfraAdminAuthServiceImpl authService;

    /** 真实密码编码器：让「密码校验」这一段真的跑起来 */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        authService = new InfraAdminAuthServiceImpl();
        ReflectionTestUtils.setField(authService, "infraUserMapper", infraUserMapper);
        ReflectionTestUtils.setField(authService, "infraTokenService", infraTokenService);
        ReflectionTestUtils.setField(authService, "infraPermissionService", infraPermissionService);
        ReflectionTestUtils.setField(authService, "infraMenuService", infraMenuService);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
    }

    /**
     * 每个用例结束后清理线程上下文，避免相互影响
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 账号不存在时不能暴露「用户不存在」，统一按登录失败处理
     */
    @Test
    @DisplayName("login：账号不存在时返回登录失败")
    void loginShouldFailWhenUserNotFound() {
        when(infraUserMapper.selectOne(any())).thenReturn(null);

        assertLoginFailed(buildLoginReq("nobody", "123456"));
    }

    /**
     * 密码错误同样按登录失败处理
     */
    @Test
    @DisplayName("login：密码错误时返回登录失败")
    void loginShouldFailWhenPasswordWrong() {
        when(infraUserMapper.selectOne(any())).thenReturn(buildUser(CommonStatusEnum.ENABLED));

        assertLoginFailed(buildLoginReq("admin", "wrong-password"));
    }

    /**
     * 账号停用要单独提示，便于使用者知道该找管理员而不是反复试密码
     */
    @Test
    @DisplayName("login：账号停用时返回用户已停用")
    void loginShouldFailWhenUserDisabled() {
        when(infraUserMapper.selectOne(any())).thenReturn(buildUser(CommonStatusEnum.DISABLED));

        assertThatThrownBy(() -> authService.login(buildLoginReq("admin", "123456"), "127.0.0.1"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.USER_DISABLED.code()));
    }

    /**
     * 登录成功时委托凭证服务签发 admin 端凭证
     */
    @Test
    @DisplayName("login：校验通过后签发 admin 端凭证")
    void loginShouldCreateAdminToken() {
        when(infraUserMapper.selectOne(any())).thenReturn(buildUser(CommonStatusEnum.ENABLED));
        when(infraTokenService.createTokenPair(anyLong(), eq(UserTypeEnum.ADMIN), anyString(), anyString()))
                .thenReturn(new AuthTokenRespVO("access", InfraConstant.TOKEN_TYPE_BEARER, 7200L, "refresh"));

        AuthTokenRespVO respVO = authService.login(buildLoginReq("admin", "123456"), "127.0.0.1");

        assertThat(respVO.getAccessToken()).isEqualTo("access");
        assertThat(respVO.getRefreshToken()).isEqualTo("refresh");
    }

    /**
     * 当前用户信息要带上角色编码与权限标识，前端据此控制按钮
     */
    @Test
    @DisplayName("getUserInfo：返回昵称、角色编码与权限标识")
    void getUserInfoShouldReturnRolesAndPerms() {
        UserContextHolder.set(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));
        when(infraUserMapper.selectById(1L)).thenReturn(buildUser(CommonStatusEnum.ENABLED));
        when(infraPermissionService.getRoleCodes(1L)).thenReturn(Set.of(InfraConstant.SUPER_ADMIN_ROLE_CODE));
        when(infraPermissionService.getPermissions(1L)).thenReturn(Set.of(InfraPermissionConstant.USER_CREATE));

        AuthUserInfoRespVO vo = authService.getUserInfo();

        assertThat(vo.getUserId()).isEqualTo(1L);
        assertThat(vo.getNickname()).isEqualTo("超级管理员");
        assertThat(vo.getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        assertThat(vo.getRoleCodes()).containsExactly(InfraConstant.SUPER_ADMIN_ROLE_CODE);
        assertThat(vo.getPerms()).containsExactly(InfraPermissionConstant.USER_CREATE);
    }

    /**
     * 原密码不正确时不允许改密
     */
    @Test
    @DisplayName("updatePassword：原密码错误时拒绝")
    void updatePasswordShouldRejectWrongOldPassword() {
        UserContextHolder.set(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));
        when(infraUserMapper.selectById(1L)).thenReturn(buildUser(CommonStatusEnum.ENABLED));
        AuthUpdatePasswordReqVO reqVO = new AuthUpdatePasswordReqVO();
        reqVO.setOldPassword("wrong");
        reqVO.setNewPassword("new-password");

        assertThatThrownBy(() -> authService.updatePassword(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.OLD_PASSWORD_ERROR.code()));
    }

    /**
     * 改密成功后新密码生效，并且该用户所有凭证失效
     */
    @Test
    @DisplayName("updatePassword：成功后新密码生效并失效全部凭证")
    void updatePasswordShouldUpdatePasswordAndRevokeTokens() {
        UserContextHolder.set(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));
        InfraUser user = buildUser(CommonStatusEnum.ENABLED);
        when(infraUserMapper.selectById(1L)).thenReturn(user);
        AuthUpdatePasswordReqVO reqVO = new AuthUpdatePasswordReqVO();
        reqVO.setOldPassword("123456");
        reqVO.setNewPassword("new-password");

        authService.updatePassword(reqVO);

        assertThat(passwordEncoder.matches("new-password", user.getPassword())).isTrue();
        verify(infraUserMapper).updateById(user);
        verify(infraTokenService).revokeAll(1L);
    }

    /**
     * 断言登录失败（错误码为登录失败）
     *
     * @param reqVO 登录入参
     */
    private void assertLoginFailed(AuthLoginReqVO reqVO) {
        assertThatThrownBy(() -> authService.login(reqVO, "127.0.0.1"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.LOGIN_FAILED.code()));
    }

    /**
     * 构造登录入参
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录入参
     */
    private AuthLoginReqVO buildLoginReq(String username, String password) {
        AuthLoginReqVO reqVO = new AuthLoginReqVO();
        reqVO.setUsername(username);
        reqVO.setPassword(password);
        return reqVO;
    }

    /**
     * 构造用户实体，密码为 123456 的 BCrypt 哈希
     *
     * @param status 状态
     * @return 用户实体
     */
    private InfraUser buildUser(CommonStatusEnum status) {
        InfraUser user = new InfraUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setNickname("超级管理员");
        user.setMobile("13800000000");
        user.setStatus(status.getValue());
        return user;
    }
}
