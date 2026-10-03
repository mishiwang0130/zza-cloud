package com.wxy.common.webmvc.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 鉴权相关配置项，前缀 {@code zza.security}。
 *
 * <p>每个服务各绑一份（配置写在各自的 yml 里），所以「模拟登录开关」「免登录白名单」可以按服务、
 * 按环境单独调，这点与芋道的 {@code yudao.security} 一致。
 *
 * <p>访问令牌的请求头名称不在这里：它固定为 {@code HeaderConstant.AUTHORIZATION}，
 * 是各服务与网关之间的契约，不该按服务或环境变化。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@ConfigurationProperties(prefix = "zza.security")
public class SecurityProperties {

    /**
     * 默认免登录路径：健康检查、接口文档、静态资源与错误转发。
     *
     * <p>放默认值是为了让每个服务引入 common-webmvc 后开箱可用；服务在 yml 里配置
     * {@code zza.security.permit-all-urls} 会**整体覆盖**这份默认值，覆盖时记得把需要的都写上
     * （例如生产想关掉文档，就只留健康检查与错误转发）。
     */
    public static final List<String> DEFAULT_PERMIT_ALL_URLS = List.of(
            "/actuator/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/favicon.ico",
            "/error");

    /**
     * 访问令牌的请求参数名，默认 {@code token}。
     *
     * <p>存在的原因是 WebSocket、SSE 这类场景没法自定义请求头，只能把令牌拼在 URL 上；
     * 普通 HTTP 接口仍然走 {@code Authorization} 头。留空表示不读请求参数。
     */
    private String tokenParameter = "token";

    /**
     * 模拟登录开关，默认关闭。每个服务单独配置，本地联调时按需打开。
     *
     * <p>打开后，以 {@link #mockSecret} 开头、后跟用户 ID 的令牌会被当成该用户的凭证，
     * 不需要真实的登录记录，方便直接调接口（尤其是调带鉴权的接口文档）。
     * <b>生产环境必须保持 false</b>，打开等于给系统留了一个后门。
     */
    private boolean mockEnable = false;

    /**
     * 模拟登录的密钥：只有以它开头的令牌才会被识别为模拟令牌，避免与真实令牌混淆。
     */
    private String mockSecret = "test";

    /**
     * 免登录路径白名单，Ant 风格（如 {@code /admin-api/open-api/**}），与接口上的 {@code @PermitAll} 注解取并集。
     *
     * <p>两个机制各管一段：单个接口免登录用注解最直观（改动就写在接口上，不用改配置）；
     * 成片路径——整个 Controller、OpenAPI 文档、健康检查、服务内部接口——用配置最省事。
     *
     * <p>没列在这里、也没标注解的接口一律要求登录。
     */
    private List<String> permitAllUrls = new ArrayList<>(DEFAULT_PERMIT_ALL_URLS);
}
