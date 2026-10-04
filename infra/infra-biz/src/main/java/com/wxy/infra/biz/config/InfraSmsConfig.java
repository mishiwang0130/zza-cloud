package com.wxy.infra.biz.config;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 短信客户端装配：把阿里云短信（号码认证服务）客户端交给 Spring 复用。
 *
 * <p>只有在注入了访问密钥 ID 时才创建客户端：本地起服务只调管理后台或用户资料时，
 * 没必要为了一个用不到的短信接口准备真实凭据，缺凭据也只影响发验证码这一个能力
 * （判断与报错在 {@code InfraSmsCodeServiceImpl} 里，与 MinIO 未配置时的处理一致）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Configuration
@EnableConfigurationProperties(InfraSmsProperties.class)
public class InfraSmsConfig {

    /**
     * 创建阿里云短信客户端
     *
     * <p>客户端构造不发网络请求，真正的鉴权与调用发生在发送时，所以这里不做连通性校验。
     *
     * @param infraSmsProperties 短信配置
     * @return 阿里云短信客户端
     * @throws Exception 客户端初始化失败
     */
    @Bean
    @ConditionalOnExpression("'${zza.sms.access-key-id:}'.length() > 0")
    public Client infraSmsClient(InfraSmsProperties infraSmsProperties) throws Exception {
        Config config = new Config()
                .setAccessKeyId(infraSmsProperties.getAccessKeyId())
                .setAccessKeySecret(infraSmsProperties.getAccessKeySecret())
                .setEndpoint(infraSmsProperties.getEndpoint());
        return new Client(config);
    }
}
