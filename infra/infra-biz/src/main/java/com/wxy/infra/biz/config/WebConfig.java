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
 * <p>拦截器注册在所有路径上，免登录一律显式声明，二选一：
 * 接口上标注 {@code jakarta.annotation.security.PermitAll}（改动就在接口上，适合单个接口），
 * 或配置 {@code zza.infra.security.permit-all-urls}（Ant 风格，适合整片路径，例如 OpenAPI 文档、
 * 健康检查、整个免登录 Controller）。没声明免登录的请求一律要求有效凭证，不存在默认放行。
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

    /**
     * 拦截所有路径：默认必须携带有效凭证，免登录由注解或 {@code zza.infra.security.permit-all-urls} 声明。
     *
     * <p>不按端前缀分别注册：端类型判定在 {@link AuthInterceptor} 里按前缀完成，
     * 统一注册才能让没有端前缀的接口也落到「默认要求登录」这一侧。
     */
    private static final String[] ALL_PATTERNS = {"/**"};

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
        registry.addInterceptor(new AuthInterceptor(infraTokenService, webProperties, infraSecurityProperties))
                .addPathPatterns(ALL_PATTERNS)
                .order(AUTH_INTERCEPTOR_ORDER);
        registry.addInterceptor(new PermissionInterceptor(infraPermissionService))
                .addPathPatterns(ALL_PATTERNS)
                .order(PERMISSION_INTERCEPTOR_ORDER);
    }
}
