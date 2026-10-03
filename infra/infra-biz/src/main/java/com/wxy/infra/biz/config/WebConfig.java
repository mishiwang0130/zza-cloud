package com.wxy.infra.biz.config;

import com.wxy.infra.biz.interceptor.PermissionInterceptor;
import com.wxy.infra.biz.service.InfraPermissionService;
import jakarta.annotation.Resource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * infra 的 Web 配置：注册接口权限拦截器。
 *
 * <p>登录凭证校验不在这里注册：由 common-webmvc 的 {@code WebMvcConfig} 统一装配
 * {@code TokenAuthInterceptor}（infra 提供了 {@code InfraTokenValidator} 这个 {@code TokenValidator} 实现，
 * 所以自动生效），免登录由接口上的 {@code @PermitAll} 注解与 {@code zza.security.permit-all-urls} 声明。
 *
 * <p>权限拦截器排在凭证拦截器之后（凭证的 order 是 10），因为它需要已确认的登录用户才能算权限。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Configuration
@EnableConfigurationProperties(InfraTokenProperties.class)
public class WebConfig implements WebMvcConfigurer {

    /**
     * 权限拦截器顺序：排在 common-webmvc 的凭证拦截器（order 10）之后。
     */
    private static final int PERMISSION_INTERCEPTOR_ORDER = 20;

    /** 权限服务 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PermissionInterceptor(infraPermissionService))
                .addPathPatterns("/**")
                .order(PERMISSION_INTERCEPTOR_ORDER);
    }
}
