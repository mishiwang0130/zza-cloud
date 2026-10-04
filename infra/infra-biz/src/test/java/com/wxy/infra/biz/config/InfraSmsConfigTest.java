package com.wxy.infra.biz.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.aliyun.dypnsapi20170525.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 短信客户端装配测试：只验证「有没有注入访问凭据」这一个条件。
 *
 * <p>不发起任何网络调用：客户端构造本身不访问阿里云，鉴权与请求都发生在发送时。
 *
 * @author wxy
 * @date 2026/10/04
 */
class InfraSmsConfigTest {

    /** 被测配置类的上下文运行器 */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(InfraSmsConfig.class);

    /**
     * 没注入环境变量时不能装配客户端：否则本地只调管理后台接口也会因为缺密钥起不来
     */
    @Test
    @DisplayName("未注入短信凭据时不装配客户端，且不影响上下文启动")
    void shouldNotCreateClientWhenAccessKeyMissing() {
        contextRunner.withPropertyValues("zza.sms.access-key-id=", "zza.sms.access-key-secret=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(Client.class);
                });
    }

    /**
     * 注入了访问密钥才装配客户端
     */
    @Test
    @DisplayName("注入短信凭据时装配客户端")
    void shouldCreateClientWhenAccessKeyPresent() {
        contextRunner.withPropertyValues("zza.sms.access-key-id=test-key",
                        "zza.sms.access-key-secret=test-secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(Client.class);
                });
    }
}
