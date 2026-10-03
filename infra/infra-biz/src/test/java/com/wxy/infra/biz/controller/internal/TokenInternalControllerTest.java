package com.wxy.infra.biz.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.vo.internal.TokenCheckReqVO;
import com.wxy.infra.biz.vo.internal.TokenCheckRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 服务内部校验接口单元测试：返回身份三要素，并支持按需比对端类型。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class TokenInternalControllerTest {

    /** 凭证服务 */
    @Mock
    private InfraTokenService infraTokenService;

    /** 被测控制器 */
    private TokenInternalController controller;

    /**
     * 装配被测控制器
     */
    @BeforeEach
    void setUp() {
        controller = new TokenInternalController();
        ReflectionTestUtils.setField(controller, "infraTokenService", infraTokenService);
    }

    /**
     * 传入端类型时按该端校验，返回身份三要素
     */
    @Test
    @DisplayName("check：传 userType 时按该端校验并返回身份")
    void shouldCheckWithUserType() {
        when(infraTokenService.validate("token", UserTypeEnum.ADMIN))
                .thenReturn(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));
        TokenCheckReqVO reqVO = buildReq("token", UserTypeEnum.ADMIN.getValue());

        Result<TokenCheckRespVO> result = controller.check(reqVO);

        assertThat(result.getCode()).isEqualTo(CommonErrorConstant.SUCCESS.code());
        assertThat(result.getData().getUserId()).isEqualTo(1L);
        assertThat(result.getData().getUserType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        assertThat(result.getData().getUsername()).isEqualTo("admin");
    }

    /**
     * 不传端类型时只校验令牌有效性
     */
    @Test
    @DisplayName("check：不传 userType 时只校验令牌有效性")
    void shouldCheckWithoutUserType() {
        when(infraTokenService.validate("token", null))
                .thenReturn(new LoginUser(2L, UserTypeEnum.APP.getValue(), "app-user"));
        TokenCheckReqVO reqVO = buildReq("token", null);

        Result<TokenCheckRespVO> result = controller.check(reqVO);

        assertThat(result.getData().getUserId()).isEqualTo(2L);
        assertThat(result.getData().getUserType()).isEqualTo(UserTypeEnum.APP.getValue());
    }

    /**
     * 构造校验入参
     *
     * @param token    令牌
     * @param userType 端类型，可以为 null
     * @return 入参
     */
    private TokenCheckReqVO buildReq(String token, Integer userType) {
        TokenCheckReqVO reqVO = new TokenCheckReqVO();
        reqVO.setToken(token);
        reqVO.setUserType(userType);
        return reqVO;
    }
}
