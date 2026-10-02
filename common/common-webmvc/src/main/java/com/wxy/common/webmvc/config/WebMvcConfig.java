package com.wxy.common.webmvc.config;

import com.wxy.common.webmvc.exception.GlobalExceptionHandler;
import com.wxy.common.webmvc.interceptor.UserContextInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Servlet 栈 Web 公共能力的统一装配入口：端前缀、登录上下文拦截器、全局异常处理、JSON 定制、接口文档。
 *
 * <p>本类是 Spring Boot 自动配置类（注册在
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}），
 * 业务服务只要依赖 common-webmvc 就会自动生效，不需要在自己的启动类上扫描 {@code com.wxy.common}。
 *
 * <p>所有 Bean 都带 {@code @ConditionalOnMissingBean}：服务想自己定制时，定义同类型 Bean 即可覆盖。
 *
 * @author wxy
 * @date 2026/10/02
 */
@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    /** Web 层配置项 */
    private final WebProperties webProperties;

    /**
     * 构造方法注入配置项
     *
     * @param webProperties Web 层配置项
     */
    public WebMvcConfig(WebProperties webProperties) {
        this.webProperties = webProperties;
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
        registry.addInterceptor(new UserContextInterceptor()).addPathPatterns("/**");
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
