package com.wxy.infra.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraSmsCodeService;
import com.wxy.infra.biz.vo.app.SmsSendCodeReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端短信验证码接口：目前用于注册、登录时验证手机号归属。
 *
 * <p>类上的 {@code /sms} 会被 common-webmvc 按包名自动加上 {@code /app-api} 前缀，
 * 最终对外路径为 {@code /app-api/sms/sendCode}。
 *
 * <p>发送验证码必须用 {@link PermitAll} 放开：此时用户还没登录，拿不到凭证。
 * 接口本身靠「同一手机号发送间隔 + 验证码有效期」两道限制防刷。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "用户端 - 短信验证码")
@RestController
@RequestMapping("/sms")
public class SmsAppController {

    /** 短信验证码服务 */
    @Resource
    private InfraSmsCodeService infraSmsCodeService;

    /**
     * 发送验证码
     *
     * @param reqVO 发送入参
     * @return 空响应
     */
    @PermitAll
    @Operation(summary = "发送短信验证码", description = "给指定手机号发送 6 位验证码；同一手机号在间隔时间内只能发送一次")
    @PostMapping("/sendCode")
    public Result<Void> sendCode(@Validated @RequestBody SmsSendCodeReqVO reqVO) {
        infraSmsCodeService.sendCode(reqVO.getMobile());
        return Result.success();
    }
}
