package com.wxy.infra.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信配置项，前缀 {@code zza.sms}。
 *
 * <p>访问凭据只能来自环境变量 {@code APP_SMS_ACCESS_KEY_ID} / {@code APP_SMS_ACCESS_KEY_SECRET}，
 * 禁止写进 YAML 与代码；endpoint、签名与模板编码属于普通配置，直接写在 yml 里。
 *
 * <p>未注入凭据时不装配短信客户端：infra 的其他接口照常可用，只有发送验证码的接口明确报
 * 「短信服务未配置」，而不是让整个服务起不来。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@ConfigurationProperties(prefix = "zza.sms")
public class InfraSmsProperties {

    /** 访问密钥 ID，从环境变量读取；为空表示未配置短信能力 */
    private String accessKeyId;

    /** 访问密钥，从环境变量读取；为空表示未配置短信能力 */
    private String accessKeySecret;

    /** 服务接入地址，短信验证码服务固定为 dypnsapi.aliyuncs.com */
    private String endpoint = "dypnsapi.aliyuncs.com";

    /** 短信签名，必须与短信控制台里审核通过的签名完全一致 */
    private String signName;

    /** 模板编码，模板参数固定为 code（验证码）与 min（有效分钟数） */
    private String templateCode;

    /** 验证码有效期（分钟），默认 5 分钟；同时作为模板参数 min 传给短信服务 */
    private int codeExpireMinutes = 5;

    /** 同一手机号两次发送之间的最小间隔（秒），默认 60 秒，避免接口被当成短信轰炸工具 */
    private long resendIntervalSeconds = 60L;

    /** 连接超时（毫秒），不配置时用 SDK 默认值 */
    private Integer connectTimeoutMs;

    /** 读取超时（毫秒），不配置时用 SDK 默认值 */
    private Integer readTimeoutMs;
}
