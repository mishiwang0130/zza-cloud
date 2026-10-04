package com.wxy.infra.biz.service;

/**
 * 短信验证码服务：生成验证码、调阿里云短信发送、校验验证码。
 *
 * <p>验证码由本服务生成并落 Redis（到期自动失效），阿里云只负责把短信投递出去；
 * 校验时以 Redis 里的值为准，不信任前端传来的任何其他信息。
 *
 * <p>app 端的注册、登录、找回密码等场景统一调 {@link #verifyCode(String, String)} 校验，
 * 校验通过后验证码立即失效，同一个验证码不能被重复使用。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface InfraSmsCodeService {

    /**
     * 给指定手机号发送验证码
     *
     * <p>同一手机号在配置的间隔时间内只允许发送一次，超频直接报错，不会真的调短信接口。
     *
     * @param mobile 手机号
     */
    void sendCode(String mobile);

    /**
     * 校验验证码
     *
     * <p>验证码不存在或已过期、验证码不正确分别报不同错误码，前端据此决定是提示「重新获取」
     * 还是「重新输入」；校验通过后立即删除验证码。
     *
     * @param mobile 手机号
     * @param code   用户提交的验证码
     */
    void verifyCode(String mobile, String code);
}
