package com.wxy.infra.biz.config;

import com.wxy.common.webmvc.config.WebProperties;
import com.wxy.infra.biz.interceptor.AuthInterceptor;
import com.wxy.infra.biz.interceptor.PermissionInterceptor;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.service.InfraTokenService;
import jakarta.annotation.Resource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * infra 的 Web 配置：在两端接口前缀下注册「凭证校验」与「权限校验」两个拦截器。
 *
 * <p>顺序不能反：凭证拦截器先确认请求是谁发的，权限拦截器再判断能不能访问。
 * 端前缀从 {@code WebProperties} 读取，与端前缀自动拼接用的是同一份配置，改前缀时不会漏改。
 *
 * <p>拦截器不维护免登录路径白名单：免登录接口在 Controller 方法上标注
 * {@code jakarta.annotation.security.PermitAll}，由 {@link AuthInterceptor} 读注解放行，
 * 新增免登录接口不需要改本类。这里排除的只有本来就不属于业务接口的路径（健康检查、接口文档、错误转发）。
 *
 * <p>只注册在 admin 与 app 两端前缀下：将来新增端（WebSocket、开放接口等）时，
 * 要么在 {@link AuthInterceptor} 里补上该前缀对应的端类型，要么让它落在「不比对端类型」的分支，
 * 不存在「不属于任何已知端就被当成某一端」的兜底。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Configuration
@EnableConfigurationProperties({InfraTokenProperties.class, InfraSecurityProperties.class})
public class WebConfig implements WebMvcConfigurer {

    /**
     * 凭证拦截器顺序：排在 common-webmvc 的登录上下文拦截器（默认 order 0）之后。
     *
     * <p>顺序有意靠后：common 的拦截器会按请求头写入上下文，本拦截器随后用「凭证解析出的身份」覆盖它，
     * 保证身份以凭证为准（请求头只能来自网关，但凭证才是权威），避免头被伪造或串号时身份错乱。
     */
    private static final int AUTH_INTERCEPTOR_ORDER = 10;

    /** 权限拦截器顺序：必须在凭证校验之后，才能拿到已确认的登录用户 */
    private static final int PERMISSION_INTERCEPTOR_ORDER = 20;

    /** 不属于业务接口的路径：健康检查、接口文档与错误转发，两端前缀下都要排除 */
    private static final String[] COMMON_EXCLUDES = {
            "/actuator/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/favicon.ico",
            "/error"
    };

    /** 凭证服务 */
    @Resource
    private InfraTokenService infraTokenService;

    /** 权限服务 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /** Web 层配置：提供端前缀 */
    @Resource
    private WebProperties webProperties;

    /** 安全配置：令牌参数名与模拟登录开关 */
    @Resource
    private InfraSecurityProperties infraSecurityProperties;

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] patterns = {
                webProperties.getAdminApiPrefix() + "/**",
                webProperties.getAppApiPrefix() + "/**"
        };
        registry.addInterceptor(new AuthInterceptor(infraTokenService, webProperties, infraSecurityProperties))
                .addPathPatterns(patterns)
                .excludePathPatterns(COMMON_EXCLUDES)
                .order(AUTH_INTERCEPTOR_ORDER);
        registry.addInterceptor(new PermissionInterceptor(infraPermissionService))
                .addPathPatterns(patterns)
                .excludePathPatterns(COMMON_EXCLUDES)
                .order(PERMISSION_INTERCEPTOR_ORDER);
    }
}
