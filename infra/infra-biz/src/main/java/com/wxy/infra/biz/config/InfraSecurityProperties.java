package com.wxy.infra.biz.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全相关配置项，前缀 {@code zza.infra.security}。
 *
 * <p>只放「与鉴权入口有关、按环境可能不同」的开关；访问令牌的请求头名称不在这里，
 * 它固定为 {@code HeaderConstant.AUTHORIZATION}——这是与网关的契约，不该按环境变化。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@ConfigurationProperties(prefix = "zza.infra.security")
public class InfraSecurityProperties {

    /**
     * 访问令牌的请求参数名，默认 {@code token}。
     *
     * <p>存在的原因是 WebSocket、SSE 这类场景没法自定义请求头，只能把令牌拼在 URL 上；
     * 普通 HTTP 接口仍然走 {@code Authorization} 头。留空表示不读请求参数。
     */
    private String tokenParameter = "token";

    /**
     * 模拟登录开关，默认关闭。
     *
     * <p><b>仅用于本地联调</b>：打开后，以 {@link #mockSecret} 开头、后跟用户 ID 的令牌
     * 会被当成该用户的凭证，不需要真实的登录记录。生产环境必须保持 {@code false}，
     * 打开等于给系统留了一个后门。
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
     * 成片路径——整个 Controller、OpenAPI 文档、健康检查、静态资源——用配置最省事，
     * 也便于按环境差异（例如生产不放行文档）调整。
     *
     * <p>这里列出的路径同样不校验令牌；没列、也没标注解的接口一律要求登录。
     */
    private List<String> permitAllUrls = new ArrayList<>();
}
