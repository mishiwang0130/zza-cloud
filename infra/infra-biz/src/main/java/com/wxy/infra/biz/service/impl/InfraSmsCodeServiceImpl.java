package com.wxy.infra.biz.service.impl;

import com.alibaba.fastjson2.JSON;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.util.DesensitizeUtil;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.infra.biz.config.InfraSmsProperties;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.service.InfraSmsCodeService;
import com.wxy.infra.biz.util.InfraRedisKeyUtil;
import jakarta.annotation.Resource;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 短信验证码服务实现：验证码自己生成、自己落 Redis，阿里云只负责投递短信。
 *
 * <p>发送顺序是「先占发送间隔 → 再调短信接口 → 成功后才写验证码」：短信没发出去就不该占住
 * 发送间隔（用户会白等一分钟），也不该留下一个用户永远收不到的验证码。
 *
 * <p>验证码用 {@link SecureRandom} 生成：验证码一旦可预测，等于没有验证手机号归属。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Service
public class InfraSmsCodeServiceImpl implements InfraSmsCodeService {

    /** 验证码位数 */
    private static final int CODE_LENGTH = 6;

    /** 验证码取值的上界：10^6，配合补零格式生成定长数字验证码 */
    private static final int CODE_BOUND = 1_000_000;

    /** 验证码补零格式，让「验证码是几位」只有一处定义 */
    private static final String CODE_FORMAT = "%0" + CODE_LENGTH + "d";

    /** 短信服务返回的成功业务码 */
    private static final String SUCCESS_CODE = "OK";

    /** HTTP 成功状态码 */
    private static final int HTTP_STATUS_SUCCESS = 200;

    /** 模板参数名：验证码 */
    private static final String TEMPLATE_PARAM_CODE = "code";

    /** 模板参数名：有效分钟数 */
    private static final String TEMPLATE_PARAM_MINUTE = "min";

    /** 安全随机数：验证码必须不可预测，不能用普通伪随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Redis 读写工具 */
    @Resource
    private RedisUtil redisUtil;

    /** 短信配置 */
    @Resource
    private InfraSmsProperties infraSmsProperties;

    /** 短信客户端：未注入访问凭据时容器里没有这个 Bean */
    @Resource
    private ObjectProvider<Client> infraSmsClientProvider;

    /**
     * 给指定手机号发送验证码
     *
     * @param mobile 手机号
     */
    @Override
    public void sendCode(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            throw new BizException(CommonErrorConstant.PARAM_ERROR, "手机号不能为空");
        }
        Client client = infraSmsClientProvider.getIfAvailable();
        if (client == null) {
            log.error("未配置短信访问凭据，无法发送验证码："
                    + "请通过环境变量注入 APP_SMS_ACCESS_KEY_ID 与 APP_SMS_ACCESS_KEY_SECRET");
            throw new BizException(InfraErrorConstant.SMS_NOT_CONFIGURED);
        }
        String limitKey = InfraRedisKeyUtil.smsCodeLimitKey(mobile);
        Boolean acquired = redisUtil.setIfAbsent(limitKey, mobile, infraSmsProperties.getResendIntervalSeconds(),
                TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BizException(InfraErrorConstant.SMS_SEND_TOO_FREQUENT);
        }
        String code = randomCode();
        try {
            doSend(client, mobile, code);
        } catch (RuntimeException ex) {
            // 短信没发出去，释放发送间隔，让用户可以立刻重试
            redisUtil.delete(limitKey);
            throw ex;
        }
        redisUtil.set(InfraRedisKeyUtil.smsCodeKey(mobile), code, expireSeconds(), TimeUnit.SECONDS);
        log.info("短信验证码已发送：mobile={}, expireMinutes={}", DesensitizeUtil.mobile(mobile),
                infraSmsProperties.getCodeExpireMinutes());
    }

    /**
     * 校验验证码
     *
     * @param mobile 手机号
     * @param code   用户提交的验证码
     */
    @Override
    public void verifyCode(String mobile, String code) {
        if (!StringUtils.hasText(mobile) || !StringUtils.hasText(code)) {
            throw new BizException(CommonErrorConstant.PARAM_ERROR, "手机号与验证码都不能为空");
        }
        String smsCodeKey = InfraRedisKeyUtil.smsCodeKey(mobile);
        String cachedCode = redisUtil.get(smsCodeKey, String.class);
        if (!StringUtils.hasText(cachedCode)) {
            throw new BizException(InfraErrorConstant.SMS_CODE_EXPIRED);
        }
        if (!cachedCode.equals(code.trim())) {
            // 输错不删除验证码，否则用户输错一位就得重新发短信；有效期本身会兜底
            throw new BizException(InfraErrorConstant.SMS_CODE_ERROR);
        }
        // 一次性使用：校验通过立即删除，同一个验证码不能重复用于多个场景
        redisUtil.delete(smsCodeKey);
    }

    /**
     * 调用阿里云短信接口投递验证码
     *
     * @param client 短信客户端
     * @param mobile 手机号
     * @param code   验证码
     */
    private void doSend(Client client, String mobile, String code) {
        String templateParam = JSON.toJSONString(Map.of(
                TEMPLATE_PARAM_CODE, code,
                TEMPLATE_PARAM_MINUTE, String.valueOf(infraSmsProperties.getCodeExpireMinutes())));
        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setSignName(infraSmsProperties.getSignName())
                .setTemplateCode(infraSmsProperties.getTemplateCode())
                .setPhoneNumber(mobile)
                .setTemplateParam(templateParam);
        RuntimeOptions runtimeOptions = new RuntimeOptions();
        if (infraSmsProperties.getConnectTimeoutMs() != null) {
            runtimeOptions.setConnectTimeout(infraSmsProperties.getConnectTimeoutMs());
        }
        if (infraSmsProperties.getReadTimeoutMs() != null) {
            runtimeOptions.setReadTimeout(infraSmsProperties.getReadTimeoutMs());
        }
        SendSmsVerifyCodeResponse response;
        try {
            response = client.sendSmsVerifyCodeWithOptions(request, runtimeOptions);
        } catch (TeaException ex) {
            log.error("短信服务调用失败：code={}, message={}", ex.getCode(), ex.getMessage(), ex);
            throw new BizException(InfraErrorConstant.SMS_SEND_ERROR);
        } catch (Exception ex) {
            log.error("短信发送异常：mobile={}", DesensitizeUtil.mobile(mobile), ex);
            throw new BizException(InfraErrorConstant.SMS_SEND_ERROR);
        }
        // 响应校验放在 try 之外：这里抛的是自己的业务异常，不必被上面的兜底分支再包一层
        Integer statusCode = response == null ? null : response.getStatusCode();
        SendSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
        if (statusCode == null || statusCode != HTTP_STATUS_SUCCESS
                || body == null || !SUCCESS_CODE.equals(body.getCode()) || !Boolean.TRUE.equals(body.getSuccess())) {
            log.error("短信发送失败：statusCode={}, code={}, message={}", statusCode,
                    body == null ? null : body.getCode(), body == null ? null : body.getMessage());
            throw new BizException(InfraErrorConstant.SMS_SEND_ERROR);
        }
    }

    /**
     * 生成定长数字验证码
     *
     * @return 验证码
     */
    private String randomCode() {
        return String.format(Locale.ROOT, CODE_FORMAT, RANDOM.nextInt(CODE_BOUND));
    }

    /**
     * 验证码有效期换算成秒
     *
     * @return 有效秒数
     */
    private long expireSeconds() {
        return TimeUnit.MINUTES.toSeconds(infraSmsProperties.getCodeExpireMinutes());
    }
}
