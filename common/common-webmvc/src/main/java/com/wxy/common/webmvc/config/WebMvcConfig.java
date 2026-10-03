package com.wxy.common.webmvc.config;

import com.wxy.common.webmvc.exception.GlobalExceptionHandler;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.common.core.security.PermissionChecker;
import com.wxy.common.webmvc.interceptor.PermissionInterceptor;
import com.wxy.common.webmvc.interceptor.TokenAuthInterceptor;
import com.wxy.common.webmvc.interceptor.TraceIdInterceptor;
import com.wxy.common.webmvc.interceptor.UserContextInterceptor;
import com.wxy.common.webmvc.security.PermissionCheckerStartupCheck;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Servlet 栈 Web 公共能力的统一装配入口：端前缀、登录上下文拦截器、全局异常处理、JSON 定制、接口文档。
 *
 * <p>本类是 Spring Boot 自动配置类（注册在
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}），
 * 业务服务只要依赖 common-webmvc 就会自动生效，不需要在自己的启动类上扫描 {@code com.wxy.common}。
 *
 * <p>所有 Bean 都带 {@code @ConditionalOnMissingBean}：服务想自己定制时，定义同类型 Bean 即可覆盖。
 *
 * <p>登录凭证校验是「必须显式声明」的：服务必须提供 {@link TokenValidator} 实现，
 * 否则启动直接失败。缺实现时打条日志然后跳过校验等于默认放行，属于安全默认值错误；
 * 确实不需要鉴权的服务，也要显式注册一个放行实现（例如
 * {@code com.wxy.common.webmvc.security.AllowAllTokenValidator}），让「不做校验」这个决定留在代码里。
 *
 * @author wxy
 * @date 2026/10/02
 */
@AutoConfiguration
@EnableConfigurationProperties({WebProperties.class, SecurityProperties.class})
public class WebMvcConfig implements WebMvcConfigurer {

    /** 链路追踪拦截器顺序：最先执行，之后的拦截器与业务代码的日志都能带上 traceId */
    private static final int TRACE_ID_INTERCEPTOR_ORDER = Ordered.HIGHEST_PRECEDENCE;

    /** 凭证拦截器顺序：排在登录上下文拦截器之后，用令牌身份覆盖请求头身份 */
    private static final int TOKEN_AUTH_INTERCEPTOR_ORDER = 10;

    /** 权限拦截器顺序：必须在凭证校验之后，才能拿到已确认的登录用户 */
    private static final int PERMISSION_INTERCEPTOR_ORDER = 20;

    /** Web 层配置项 */
    private final WebProperties webProperties;

    /** 鉴权配置项 */
    private final SecurityProperties securityProperties;

    /** 令牌校验器：各服务自己实现，没提供时不装配凭证拦截器 */
    private final ObjectProvider<TokenValidator> tokenValidatorProvider;

    /** 权限校验器：各服务自己实现，用到权限注解就必须提供 */
    private final ObjectProvider<PermissionChecker> permissionCheckerProvider;

    /**
     * 构造方法注入配置项
     *
     * @param webProperties             Web 层配置项
     * @param securityProperties        鉴权配置项
     * @param tokenValidatorProvider    令牌校验器提供者
     * @param permissionCheckerProvider 权限校验器提供者
     */
    public WebMvcConfig(WebProperties webProperties, SecurityProperties securityProperties,
                        ObjectProvider<TokenValidator> tokenValidatorProvider,
                        ObjectProvider<PermissionChecker> permissionCheckerProvider) {
        this.webProperties = webProperties;
        this.securityProperties = securityProperties;
        this.tokenValidatorProvider = tokenValidatorProvider;
        this.permissionCheckerProvider = permissionCheckerProvider;
    }

    /**
     * 按 Controller 所在包名自动拼接端前缀
     *
     * <p>包名包含 {@code .controller.admin} 的走 {@code /admin-api}，
     * 包含 {@code .controller.app} 的走 {@code /app-api}；
     * 其余 Controller（例如服务内部接口）不加前缀，由服务自行决定路径。
     *
     * @param configurer 路径匹配配置
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(webProperties.getAdminApiPrefix(),
                clazz -> matchesPackage(clazz, webProperties.getAdminControllerPackage()));
        configurer.addPathPrefix(webProperties.getAppApiPrefix(),
                clazz -> matchesPackage(clazz, webProperties.getAppControllerPackage()));
    }

    /**
     * 注册登录上下文拦截器
     *
     * <p>拦截所有路径：没有携带用户头的请求（例如服务间内部调用、健康检查）不会报错，
     * 只是上下文为空，审计字段退化为系统用户 {@code 0}。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TraceIdInterceptor())
                .addPathPatterns("/**")
                .order(TRACE_ID_INTERCEPTOR_ORDER);
        registry.addInterceptor(new UserContextInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(new TokenAuthInterceptor(requireTokenValidator(), webProperties, securityProperties))
                .addPathPatterns("/**")
                .order(TOKEN_AUTH_INTERCEPTOR_ORDER);
        registry.addInterceptor(new PermissionInterceptor(permissionCheckerProvider.getIfAvailable()))
                .addPathPatterns("/**")
                .order(PERMISSION_INTERCEPTOR_ORDER);
    }

    /**
     * 注册权限实现的启动检查：有接口标注 {@code @RequiresPermission} 就必须提供 {@link PermissionChecker}
     *
     * @param handlerMappingProvider 控制器映射提供者
     * @return 启动检查
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionCheckerStartupCheck permissionCheckerStartupCheck(
            ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) {
        return new PermissionCheckerStartupCheck(handlerMappingProvider, permissionCheckerProvider);
    }

    /**
     * 取令牌校验实现：必须有，没有就启动失败
     *
     * <p>不做「找不到就跳过校验」的兜底：那样服务会在毫无提示的情况下变成匿名可访问，
     * 出问题时只能靠翻配置猜。启动失败的信息里直接写清楚两种做法，省掉排查成本。
     *
     * @return 令牌校验器
     */
    private TokenValidator requireTokenValidator() {
        TokenValidator tokenValidator = tokenValidatorProvider.getIfAvailable();
        if (tokenValidator == null) {
            throw new IllegalStateException("服务未提供 TokenValidator 实现，无法校验登录凭证："
                    + "需要鉴权请实现 com.wxy.common.core.security.TokenValidator"
                    + "（可参考 infra 的 InfraTokenValidator）；"
                    + "确实不需要鉴权请显式注册 com.wxy.common.webmvc.security.AllowAllTokenValidator 这类放行实现");
        }
        return tokenValidator;
    }

    /**
     * 注册全局异常处理器
     *
     * @return 全局异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 注册 Long 转字符串的 Jackson 定制
     *
     * @return Jackson 定制器
     */
    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return new JacksonConfig();
    }

    /**
     * 注册接口文档的基础信息
     *
     * <p>Knife4j 与 springdoc 只有检测到该 Bean 才会渲染文档页；
     * 服务想改标题、版本时定义同类型 Bean 即可覆盖。
     *
     * @return OpenAPI 元信息
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OpenAPI.class)
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("zza-cloud 接口文档")
                .description("统一响应规范：HTTP 200 + code/msg/data 三段式；业务失败也返回 200，由 code 表达")
                .version("1.0.0"));
    }

    /**
     * 判断类型所在包名是否包含指定片段
     *
     * @param clazz          目标类型
     * @param packageSegment 包名标识片段，为空时视为不匹配
     * @return 匹配返回 true
     */
    private static boolean matchesPackage(Class<?> clazz, String packageSegment) {
        if (packageSegment == null || packageSegment.isBlank()) {
            return false;
        }
        return clazz.getPackageName().contains(packageSegment);
    }
}
