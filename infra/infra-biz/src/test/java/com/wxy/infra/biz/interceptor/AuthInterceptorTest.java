package com.wxy.infra.biz.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.infra.biz.service.InfraTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 凭证拦截器单元测试：端类型来自注册时的绑定，而不是请求期推断。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    /** 凭证服务 */
    @Mock
    private InfraTokenService infraTokenService;

    /**
     * 清理线程上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 用注册时绑定的端类型校验凭证，并且完全不关心请求路径
     */
    @Test
    @DisplayName("preHandle：使用绑定的端类型校验，不读请求路径")
    void shouldUseBoundUserTypeWithoutResolvingPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HeaderConstant.AUTHORIZATION)).thenReturn("Bearer app-token");
        when(infraTokenService.validate("Bearer app-token", UserTypeEnum.APP))
                .thenReturn(new LoginUser(7L, UserTypeEnum.APP.getValue(), "app-user"));

        boolean handled = new AuthInterceptor(infraTokenService, UserTypeEnum.APP)
                .preHandle(request, null, new Object());

        assertThat(handled).isTrue();
        assertThat(UserContextHolder.getUserId()).isEqualTo(7L);
        assertThat(UserContextHolder.getUserType()).isEqualTo(UserTypeEnum.APP.getValue());
        // 端类型一旦按路径推断，就会出现「新端被当成 app 端」的问题，这里显式锁死不读路径
        verify(request, never()).getRequestURI();
    }
}
