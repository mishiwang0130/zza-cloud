package com.wxy.common.webmvc.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.common.core.security.PermissionChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 * Web 公共装配单元测试：确认「服务必须显式提供 TokenValidator」这条约定。
 *
 * <p>锁的是安全默认值：缺少实现时必须启动失败，而不是打条日志然后默认放行。
 *
 * @author wxy
 * @date 2026/10/03
 */
class WebMvcConfigTest {

    /**
     * 缺少令牌校验实现时启动失败，并在提示里写清两种做法
     */
    @Test
    @DisplayName("addInterceptors：缺少 TokenValidator 实现时启动失败")
    void shouldFailWhenTokenValidatorMissing() {
        WebMvcConfig webMvcConfig = buildConfig(new DefaultListableBeanFactory());

        assertThatThrownBy(() -> webMvcConfig.addInterceptors(new InterceptorRegistry()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TokenValidator")
                .hasMessageContaining("AllowAllTokenValidator");
    }

    /**
     * 提供了实现时正常注册（含放行实现这种显式声明）
     */
    @Test
    @DisplayName("addInterceptors：提供了 TokenValidator 实现时正常注册")
    void shouldRegisterWhenTokenValidatorPresent() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("tokenValidator",
                (TokenValidator) token -> new LoginUser(1L, UserTypeEnum.ADMIN.getValue(), "admin"));

        WebMvcConfig webMvcConfig = buildConfig(beanFactory);

        assertThatCode(() -> webMvcConfig.addInterceptors(new InterceptorRegistry()))
                .doesNotThrowAnyException();
    }

    /**
     * 构造被测配置
     *
     * @param beanFactory Bean 工厂，用于提供 TokenValidator
     * @return 被测配置
     */
    private WebMvcConfig buildConfig(DefaultListableBeanFactory beanFactory) {
        return new WebMvcConfig(new WebProperties(), new SecurityProperties(),
                beanFactory.getBeanProvider(TokenValidator.class),
                beanFactory.getBeanProvider(PermissionChecker.class));
    }
}
