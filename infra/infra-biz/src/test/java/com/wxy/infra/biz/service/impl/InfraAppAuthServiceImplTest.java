package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.CommonStatusEnum;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.infra.biz.mapper.InfraAppUserMapper;
import com.wxy.infra.biz.po.InfraAppUser;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.service.InfraSmsCodeService;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.FileRespVO;
import com.wxy.infra.biz.vo.app.AppAuthUserInfoRespVO;
import com.wxy.infra.biz.vo.app.AuthAppRefreshReqVO;
import com.wxy.infra.biz.vo.app.AuthAppUpdateProfileReqVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 用户端认证服务单元测试：续期端类型、登出委托、个人资料查询与修改。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class InfraAppAuthServiceImplTest {

    /** 短信验证码服务 */
    @Mock
    private InfraSmsCodeService infraSmsCodeService;

    /** 用户端用户 Mapper */
    @Mock
    private InfraAppUserMapper infraAppUserMapper;

    /** 凭证服务 */
    @Mock
    private InfraTokenService infraTokenService;

    /** 文件服务 */
    @Mock
    private InfraFileService infraFileService;

    /** 被测服务 */
    private InfraAppAuthServiceImpl appAuthService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appAuthService = new InfraAppAuthServiceImpl();
        ReflectionTestUtils.setField(appAuthService, "infraSmsCodeService", infraSmsCodeService);
        ReflectionTestUtils.setField(appAuthService, "infraAppUserMapper", infraAppUserMapper);
        ReflectionTestUtils.setField(appAuthService, "infraTokenService", infraTokenService);
        ReflectionTestUtils.setField(appAuthService, "infraFileService", infraFileService);
    }

    /**
     * 清理登录上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 续期固定按用户端（user_type=2）处理
     */
    @Test
    @DisplayName("refresh：固定按用户端续期")
    void refreshShouldUseAppUserType() {
        AuthAppRefreshReqVO reqVO = new AuthAppRefreshReqVO();
        reqVO.setRefreshToken("refresh-1");
        AuthTokenRespVO token = new AuthTokenRespVO("access", "Bearer", 7200L, "refresh-2");
        when(infraTokenService.refresh("refresh-1", UserTypeEnum.APP)).thenReturn(token);

        AuthTokenRespVO result = appAuthService.refresh(reqVO);

        assertThat(result).isSameAs(token);
    }

    /**
     * 登出委托凭证服务，同时失效访问凭证与同一会话的续期凭证
     */
    @Test
    @DisplayName("logout：委托凭证服务失效")
    void logoutShouldDelegateToRevoke() {
        appAuthService.logout("Bearer access");

        verify(infraTokenService).revoke("Bearer access");
    }

    /**
     * 查询个人资料：手机号脱敏、头像换成预签名地址
     */
    @Test
    @DisplayName("getUserInfo：手机号脱敏并回填头像地址")
    void getUserInfoShouldDesensitizeMobileAndFillAvatarUrl() {
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
        when(infraAppUserMapper.selectById(100L)).thenReturn(buildUser(66L));
        FileRespVO file = new FileRespVO();
        file.setId(66L);
        file.setUrl("http://minio/avatar");
        when(infraFileService.listByIds(List.of(66L))).thenReturn(List.of(file));

        AppAuthUserInfoRespVO result = appAuthService.getUserInfo();

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getMobile()).isEqualTo("138****0000");
        assertThat(result.getNickname()).isEqualTo("测试租客");
        assertThat(result.getAvatarFileId()).isEqualTo(66L);
        assertThat(result.getAvatarUrl()).isEqualTo("http://minio/avatar");
    }

    /**
     * 查询个人资料：未设置头像（fileId=0）时地址为 null，且不去查文件
     */
    @Test
    @DisplayName("getUserInfo：未设置头像时地址为 null")
    void getUserInfoShouldReturnNullAvatarUrlWhenNotSet() {
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
        when(infraAppUserMapper.selectById(100L)).thenReturn(buildUser(0L));

        AppAuthUserInfoRespVO result = appAuthService.getUserInfo();

        assertThat(result.getAvatarUrl()).isNull();
        verifyNoInteractions(infraFileService);
    }

    /**
     * 未登录时查询个人资料按 401 处理
     */
    @Test
    @DisplayName("getUserInfo：未登录时抛未登录异常")
    void getUserInfoShouldRejectAnonymous() {
        assertThatThrownBy(() -> appAuthService.getUserInfo())
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 修改资料：昵称去首尾空格，头像传 0 表示清空
     */
    @Test
    @DisplayName("updateProfile：昵称去空格、头像传 0 清空")
    void updateProfileShouldTrimNicknameAndClearAvatar() {
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
        InfraAppUser user = buildUser(66L);
        when(infraAppUserMapper.selectById(100L)).thenReturn(user);
        AuthAppUpdateProfileReqVO reqVO = new AuthAppUpdateProfileReqVO();
        reqVO.setNickname("  新昵称  ");
        reqVO.setAvatarFileId(0L);

        appAuthService.updateProfile(reqVO);

        assertThat(user.getNickname()).isEqualTo("新昵称");
        assertThat(user.getAvatarFileId()).isZero();
        verify(infraAppUserMapper).updateById(user);
    }

    /**
     * 修改资料：只填空格等于没填，按参数错误拒绝
     */
    @Test
    @DisplayName("updateProfile：空白昵称按参数错误拒绝")
    void updateProfileShouldRejectBlankNickname() {
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
        when(infraAppUserMapper.selectById(100L)).thenReturn(buildUser(0L));
        AuthAppUpdateProfileReqVO reqVO = new AuthAppUpdateProfileReqVO();
        reqVO.setNickname("   ");
        reqVO.setAvatarFileId(null);

        assertThatThrownBy(() -> appAuthService.updateProfile(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorConstant.PARAM_ERROR.code()));
        verify(infraAppUserMapper, never()).updateById(any(InfraAppUser.class));
    }

    /**
     * 构造用户端用户实体
     *
     * @param avatarFileId 头像文件 ID，0 表示未设置
     * @return 用户实体
     */
    private InfraAppUser buildUser(Long avatarFileId) {
        InfraAppUser user = new InfraAppUser();
        user.setId(100L);
        user.setMobile("13800000000");
        user.setNickname("测试租客");
        user.setAvatarFileId(avatarFileId);
        user.setStatus(CommonStatusEnum.ENABLED.getValue());
        return user;
    }
}
