package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.infra.biz.service.InfraTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * infra 的令牌校验实现单元测试：确认它只是把公共 SPI 接到本服务的凭证服务上，且不比对端类型。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class InfraTokenValidatorTest {

    /** 凭证服务 */
    @Mock
    private InfraTokenService infraTokenService;

    /** 被测实现 */
    private InfraTokenValidator infraTokenValidator;

    /**
     * 装配被测实现
     */
    @BeforeEach
    void setUp() {
        infraTokenValidator = new InfraTokenValidator();
        ReflectionTestUtils.setField(infraTokenValidator, "infraTokenService", infraTokenService);
    }

    /**
     * 校验委托给凭证服务，并且不传期望端类型（端比对由公共拦截器统一做）
     */
    @Test
    @DisplayName("validate：委托凭证服务且不比对端类型")
    void shouldDelegateWithoutUserType() {
        when(infraTokenService.validate("token", null))
                .thenReturn(new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));

        LoginUser loginUser = infraTokenValidator.validate("token");

        assertThat(loginUser.userId()).isEqualTo(1L);
        assertThat(loginUser.userType()).isEqualTo(UserTypeEnum.ADMIN.getValue());
        verify(infraTokenService).validate("token", null);
    }
}
