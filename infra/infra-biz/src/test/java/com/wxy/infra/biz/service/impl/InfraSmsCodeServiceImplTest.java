package com.wxy.infra.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.infra.biz.config.InfraSmsProperties;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.util.InfraRedisKeyUtil;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 短信验证码服务单元测试：未配置凭据的降级、发送间隔限制、发送成功写缓存、发送失败释放间隔与验证码校验。
 *
 * <p>只 mock Redis 与短信客户端，不依赖真实 Redis、网络与短信凭据。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class InfraSmsCodeServiceImplTest {

    /** 测试手机号 */
    private static final String MOBILE = "13800000000";

    /** 测试验证码 */
    private static final String CODE = "123456";

    /** Redis 读写工具 */
    @Mock
    private RedisUtil redisUtil;

    /** 短信客户端提供者 */
    @Mock
    private ObjectProvider<Client> infraSmsClientProvider;

    /** 短信客户端 */
    @Mock
    private Client infraSmsClient;

    /** 被测服务 */
    private InfraSmsCodeServiceImpl smsCodeService;

    /**
     * 装配被测服务：配置项用真实对象，避免把「默认 5 分钟」这类默认值也 mock 掉
     */
    @BeforeEach
    void setUp() {
        InfraSmsProperties properties = new InfraSmsProperties();
        properties.setSignName("测试签名");
        properties.setTemplateCode("100001");
        properties.setCodeExpireMinutes(5);
        properties.setResendIntervalSeconds(60);

        smsCodeService = new InfraSmsCodeServiceImpl();
        ReflectionTestUtils.setField(smsCodeService, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(smsCodeService, "infraSmsProperties", properties);
        ReflectionTestUtils.setField(smsCodeService, "infraSmsClientProvider", infraSmsClientProvider);
    }

    /**
     * 没注入环境变量时不装配客户端，此时发短信要报明确的「未配置」，而不是抛空指针
     */
    @Test
    @DisplayName("sendCode：未注入短信凭据时报「短信服务未配置」且不占用发送间隔")
    void sendCodeShouldFailWhenNotConfigured() {
        when(infraSmsClientProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> smsCodeService.sendCode(MOBILE))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.SMS_NOT_CONFIGURED.code()));
        verify(redisUtil, never()).setIfAbsent(anyString(), any(), anyLong(), any());
    }

    /**
     * 发送成功时才写验证码：模板参数里的验证码必须与缓存里的一致
     */
    @Test
    @DisplayName("sendCode：发送成功后按配置的分钟数缓存验证码，模板参数带同一个验证码")
    void sendCodeShouldCacheCodeAfterSendSuccess() throws Exception {
        when(infraSmsClientProvider.getIfAvailable()).thenReturn(infraSmsClient);
        when(redisUtil.setIfAbsent(eq(InfraRedisKeyUtil.smsCodeLimitKey(MOBILE)), any(), eq(60L),
                eq(TimeUnit.SECONDS))).thenReturn(true);
        when(infraSmsClient.sendSmsVerifyCodeWithOptions(any(), any())).thenReturn(successResponse());

        smsCodeService.sendCode(MOBILE);

        ArgumentCaptor<SendSmsVerifyCodeRequest> requestCaptor = ArgumentCaptor.forClass(SendSmsVerifyCodeRequest.class);
        verify(infraSmsClient).sendSmsVerifyCodeWithOptions(requestCaptor.capture(), any());
        assertThat(requestCaptor.getValue().getPhoneNumber()).isEqualTo(MOBILE);
        assertThat(requestCaptor.getValue().getSignName()).isEqualTo("测试签名");
        assertThat(requestCaptor.getValue().getTemplateCode()).isEqualTo("100001");

        ArgumentCaptor<Object> codeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(redisUtil).set(eq(InfraRedisKeyUtil.smsCodeKey(MOBILE)), codeCaptor.capture(), eq(300L),
                eq(TimeUnit.SECONDS));
        String cachedCode = String.valueOf(codeCaptor.getValue());
        assertThat(cachedCode).hasSize(6);
        assertThat(requestCaptor.getValue().getTemplateParam()).contains(cachedCode);
    }

    /**
     * 发送间隔内重复请求直接拒绝，不能真的调短信接口（否则接口会被当成短信轰炸工具）
     */
    @Test
    @DisplayName("sendCode：发送间隔内的重复请求报「发送过于频繁」且不调短信接口")
    void sendCodeShouldRejectFrequentRequest() throws Exception {
        when(infraSmsClientProvider.getIfAvailable()).thenReturn(infraSmsClient);
        when(redisUtil.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(false);

        assertThatThrownBy(() -> smsCodeService.sendCode(MOBILE))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.SMS_SEND_TOO_FREQUENT.code()));
        verify(infraSmsClient, never()).sendSmsVerifyCodeWithOptions(any(), any());
        verify(redisUtil, never()).set(anyString(), any(), anyLong(), any());
    }

    /**
     * 发送失败要释放发送间隔：否则用户重试会白等一分钟，还拿不到验证码
     */
    @Test
    @DisplayName("sendCode：发送失败时报「短信发送失败」并释放发送间隔，不缓存验证码")
    void sendCodeShouldReleaseLimitWhenSendFails() throws Exception {
        when(infraSmsClientProvider.getIfAvailable()).thenReturn(infraSmsClient);
        when(redisUtil.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);
        when(infraSmsClient.sendSmsVerifyCodeWithOptions(any(), any()))
                .thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> smsCodeService.sendCode(MOBILE))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.SMS_SEND_ERROR.code()));
        verify(redisUtil).delete(InfraRedisKeyUtil.smsCodeLimitKey(MOBILE));
        verify(redisUtil, never()).set(anyString(), any(), anyLong(), any());
    }

    /**
     * HTTP 200 但业务码不是 OK 也算失败：不能只看状态码就把验证码写进缓存
     */
    @Test
    @DisplayName("sendCode：阿里云返回失败业务码时报「短信发送失败」")
    void sendCodeShouldFailWhenResponseNotOk() throws Exception {
        when(infraSmsClientProvider.getIfAvailable()).thenReturn(infraSmsClient);
        when(redisUtil.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);
        when(infraSmsClient.sendSmsVerifyCodeWithOptions(any(), any())).thenReturn(
                new SendSmsVerifyCodeResponse().setStatusCode(200).setBody(
                        new SendSmsVerifyCodeResponseBody().setCode("isv.SMS_TEMPLATE_ILLEGAL").setSuccess(false)));

        assertThatThrownBy(() -> smsCodeService.sendCode(MOBILE))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.SMS_SEND_ERROR.code()));
        verify(redisUtil, never()).set(anyString(), any(), anyLong(), any());
    }

    /**
     * 验证码正确时校验通过并立即失效，防止同一个验证码被重复提交
     */
    @Test
    @DisplayName("verifyCode：验证码正确时校验通过并删除缓存")
    void verifyCodeShouldPassAndDeleteCache() {
        when(redisUtil.get(InfraRedisKeyUtil.smsCodeKey(MOBILE), String.class)).thenReturn(CODE);

        smsCodeService.verifyCode(MOBILE, CODE);

        verify(redisUtil).delete(InfraRedisKeyUtil.smsCodeKey(MOBILE));
    }

    /**
     * 验证码不存在或已过期时报「已过期」，前端据此引导重新获取
     */
    @Test
    @DisplayName("verifyCode：缓存里没有验证码时报「验证码已过期」")
    void verifyCodeShouldFailWhenCodeExpired() {
        when(redisUtil.get(InfraRedisKeyUtil.smsCodeKey(MOBILE), String.class)).thenReturn(null);

        assertThatThrownBy(() -> smsCodeService.verifyCode(MOBILE, CODE))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.SMS_CODE_EXPIRED.code()));
        verify(redisUtil, never()).delete(anyString());
    }

    /**
     * 验证码输错不删除缓存：输错一位就作废会让用户不得不重新发短信
     */
    @Test
    @DisplayName("verifyCode：验证码不正确时报「验证码不正确」且保留缓存")
    void verifyCodeShouldFailWhenCodeMismatch() {
        when(redisUtil.get(InfraRedisKeyUtil.smsCodeKey(MOBILE), String.class)).thenReturn(CODE);

        assertThatThrownBy(() -> smsCodeService.verifyCode(MOBILE, "000000"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(InfraErrorConstant.SMS_CODE_ERROR.code()));
        verify(redisUtil, never()).delete(anyString());
    }

    /**
     * 构造一条阿里云返回的成功响应
     *
     * @return 成功响应
     */
    private SendSmsVerifyCodeResponse successResponse() {
        return new SendSmsVerifyCodeResponse()
                .setStatusCode(200)
                .setBody(new SendSmsVerifyCodeResponseBody().setCode("OK").setSuccess(true));
    }
}
