package com.wxy.common.webmvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web 层配置项：端前缀与包名的对应关系，由 {@link WebMvcConfig} 读取并生效。
 *
 * <p>Controller 上只写业务路径（如 {@code @RequestMapping("/user")}），
 * 端前缀由配置按包名自动拼接，避免手写 {@code /admin-api} 写漏或写重。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Data
@ConfigurationProperties(prefix = "zza.web")
public class WebProperties {

    /** 管理后台接口前缀 */
    private String adminApiPrefix = "/admin-api";

    /** 管理后台 Controller 所在包的标识片段，包名包含它即认为是管理后台接口 */
    private String adminControllerPackage = ".controller.admin";

    /** 用户端接口前缀 */
    private String appApiPrefix = "/app-api";

    /** 用户端 Controller 所在包的标识片段，包名包含它即认为是用户端接口 */
    private String appControllerPackage = ".controller.app";
}
