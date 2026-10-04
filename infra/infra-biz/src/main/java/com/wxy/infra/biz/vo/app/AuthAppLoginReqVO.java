package com.wxy.infra.biz.vo.app;

import com.wxy.common.webmvc.validation.Mobile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户端登录请求：手机号 + 短信验证码。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AuthAppLoginReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录手机号：app 端以手机号为账号 */
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    /** 短信验证码：6 位数字，有效期与发送间隔见 zza.sms 配置 */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码为 6 位数字")
    private String code;
}