package com.wxy.common.security.defaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraPermissionClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 默认权限校验实现单元测试：结果透传，异常/熔断一律按无权限处理。
 *
 * @author wxy
 * @date 2026/10/03
 */
@ExtendWith(MockitoExtension.class)
class DefaultPermissionCheckerTest {

    /** infra 权限服务客户端 */
    @Mock
    private InfraPermissionClient infraPermissionClient;

    /** 被测实现 */
    private DefaultPermissionChecker checker;

    /**
     * 装配被测实现
     */
    @BeforeEach
    void setUp() {
        checker = new DefaultPermissionChecker(infraPermissionClient);
    }

    /**
     * 远端返回有权限时放行
     */
    @Test
    @DisplayName("hasAnyPermission：远端返回有权限时放行")
    void shouldPassWhenRemoteAllows() {
        when(infraPermissionClient.hasAnyPermission(any())).thenReturn(Result.success(true));

        assertThat(checker.hasAnyPermission(adminUser(), List.of("infra:user:create"))).isTrue();
    }

    /**
     * 远端返回 null 数据时按无权限处理，不做「非 false 即通过」的判断
     */
    @Test
    @DisplayName("hasAnyPermission：远端返回空数据时按无权限处理")
    void shouldRejectWhenRemoteDataNull() {
        when(infraPermissionClient.hasAnyPermission(any())).thenReturn(Result.success(null));

        assertThat(checker.hasAnyPermission(adminUser(), List.of("infra:user:create"))).isFalse();
    }

    /**
     * 远端返回失败结果时抛异常，绝不当成「有权限」
     *
     * <p>调用失败与被熔断由客户端的降级工厂兜住（返回无权限），这里覆盖业务失败响应的情形。
     */
    @Test
    @DisplayName("hasAnyPermission：远端返回失败结果时抛异常")
    void shouldThrowWhenRemoteReturnsError() {
        when(infraPermissionClient.hasAnyPermission(any()))
                .thenReturn(Result.error(CommonErrorConstant.REMOTE_CALL_ERROR));

        assertThatThrownBy(() -> checker.hasAnyPermission(adminUser(), List.of("infra:user:create")))
                .isInstanceOf(BizException.class);
    }

    /**
     * 构造管理后台登录用户
     *
     * @return 登录用户
     */
    private LoginUser adminUser() {
        return new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin");
    }
}
