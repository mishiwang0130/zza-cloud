package com.wxy.infra.biz.config;

import com.wxy.common.webmvc.config.WebProperties;
import com.wxy.infra.biz.interceptor.AuthInterceptor;
import com.wxy.infra.biz.interceptor.PermissionInterceptor;
import com.wxy.infra.biz.service.InfraPermissionService;
import com.wxy.infra.biz.service.InfraTokenService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * infra 的 Web 配置：注册「凭证校验」与「权限校验」两个拦截器。
 *
 * <p>顺序不能反：凭证拦截器（order 0）先确认请求是谁发的，权限拦截器（order 1）再判断能不能访问。
 * 两端前缀从 {@code WebProperties} 读取，与端前缀自动拼接使用同一份配置，避免前缀改了这里漏改。
 *
 * <p>免登录白名单包含两端各自的登录与续期接口（app 端接口后续补齐时无需再改本类），
 * 以及健康检查、接口文档与错误转发路径。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Configuration
@EnableConfigurationProperties(InfraTokenProperties.class)
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

    /** 凭证服务 */
    @Resource
    private InfraTokenService infraTokenService;

    /** 权限服务 */
    @Resource
    private InfraPermissionService infraPermissionService;

    /** Web 层配置：提供端前缀 */
    @Resource
    private WebProperties webProperties;

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] patterns = buildAuthPatterns();
        String[] excludes = buildExcludes();
        registry.addInterceptor(new AuthInterceptor(infraTokenService, webProperties))
                .addPathPatterns(patterns)
                .excludePathPatterns(excludes)
                .order(AUTH_INTERCEPTOR_ORDER);
        registry.addInterceptor(new PermissionInterceptor(infraPermissionService))
                .addPathPatterns(patterns)
                .excludePathPatterns(excludes)
                .order(PERMISSION_INTERCEPTOR_ORDER);
    }

    /**
     * 需要校验凭证的路径：两端前缀下的所有接口
     *
     * @return 路径模式数组
     */
    private String[] buildAuthPatterns() {
        return new String[]{
                webProperties.getAdminApiPrefix() + "/**",
                webProperties.getAppApiPrefix() + "/**"
        };
    }

    /**
     * 免登录白名单：两端的登录与续期接口 + 健康检查与接口文档
     *
     * @return 排除路径数组
     */
    private String[] buildExcludes() {
        List<String> excludes = new ArrayList<>();
        for (String prefix : List.of(webProperties.getAdminApiPrefix(), webProperties.getAppApiPrefix())) {
            excludes.add(prefix + "/auth/login");
            excludes.add(prefix + "/auth/refresh");
        }
        excludes.add("/actuator/**");
        excludes.add("/doc.html");
        excludes.add("/webjars/**");
        excludes.add("/v3/api-docs/**");
        excludes.add("/swagger-ui/**");
        excludes.add("/favicon.ico");
        excludes.add("/error");
        return excludes.toArray(new String[0]);
    }
}
