package com.wxy.infra.biz.config;

import com.wxy.common.core.enums.UserTypeEnum;
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
 * <p>顺序不能反：凭证拦截器先确认请求是谁发的，权限拦截器再判断能不能访问。
 * 端前缀从 {@code WebProperties} 读取，与端前缀自动拼接使用同一份配置，避免前缀改了这里漏改。
 *
 * <p><b>按端注册</b>：每一端注册一组自己的拦截器，期望的登录端类型在注册时就绑定好。
 * 刻意不做「不是 admin 前缀就算 app 端」的兜底推断——那样新增端（内部接口、开放平台等）
 * 会被静默按 app 端校验。新增端时在这里再调一次 {@link #addEndInterceptors} 即可，
 * 未注册前缀的请求不会被任何凭证拦截器覆盖，因此新增端必须显式注册。
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

    /** 与端无关的免登录路径：健康检查、接口文档、错误转发 */
    private static final List<String> COMMON_EXCLUDES = List.of(
            "/actuator/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/favicon.ico",
            "/error");

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
        for (EndApi endApi : endApis()) {
            addEndInterceptors(registry, endApi.apiPrefix(), endApi.userType());
        }
    }

    /**
     * 各端的拦截器配置：一个端 = 一个接口前缀 + 一个要求的登录端类型
     *
     * <p>端类型与端前缀在这里一一对应地写清楚，新增端时补一项即可（同时补
     * {@code UserTypeEnum} 的端类型与 {@code WebProperties} 的前缀配置）。
     * 这样新增端不可能被漏成「按 app 端校验」：有配置才有校验，没有配置就没有兜底。
     *
     * @return 端配置列表
     */
    List<EndApi> endApis() {
        return List.of(
                new EndApi(webProperties.getAdminApiPrefix(), UserTypeEnum.ADMIN),
                new EndApi(webProperties.getAppApiPrefix(), UserTypeEnum.APP));
    }

    /**
     * 为某一端注册凭证与权限拦截器
     *
     * <p>新增端（例如内部接口 {@code /internal-api}）时在这里再加一行调用：
     * 需要同时在 {@code UserTypeEnum} 里补一个端类型、在 {@code WebProperties} 里补一个前缀配置。
     *
     * @param registry  拦截器注册表
     * @param apiPrefix 该端的接口前缀
     * @param userType  该端要求登录用户携带的端类型
     */
    private void addEndInterceptors(InterceptorRegistry registry, String apiPrefix, UserTypeEnum userType) {
        String[] patterns = {apiPrefix + "/**"};
        String[] excludes = buildExcludes(apiPrefix);
        registry.addInterceptor(new AuthInterceptor(infraTokenService, userType))
                .addPathPatterns(patterns)
                .excludePathPatterns(excludes)
                .order(AUTH_INTERCEPTOR_ORDER);
        registry.addInterceptor(new PermissionInterceptor(infraPermissionService))
                .addPathPatterns(patterns)
                .excludePathPatterns(excludes)
                .order(PERMISSION_INTERCEPTOR_ORDER);
    }

    /**
     * 免登录白名单：本端的登录与续期接口 + 与端无关的健康检查与接口文档
     *
     * @param apiPrefix 该端的接口前缀
     * @return 排除路径数组
     */
    private String[] buildExcludes(String apiPrefix) {
        List<String> excludes = new ArrayList<>();
        excludes.add(apiPrefix + "/auth/login");
        excludes.add(apiPrefix + "/auth/refresh");
        excludes.addAll(COMMON_EXCLUDES);
        return excludes.toArray(new String[0]);
    }

    /**
     * 端配置：接口前缀与要求的登录端类型
     *
     * @param apiPrefix 该端的接口前缀，例如 {@code /admin-api}
     * @param userType  该端要求登录用户携带的端类型
     */
    record EndApi(String apiPrefix, UserTypeEnum userType) {
    }
}
