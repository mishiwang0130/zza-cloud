package com.wxy.infra.biz.vo.app;

import com.wxy.common.webmvc.validation.Mobile;
import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户端发送短信验证码的请求体。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class SmsSendCodeReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 接收验证码的手机号：必填，且必须符合中国大陆手机号格式 */
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;
}
