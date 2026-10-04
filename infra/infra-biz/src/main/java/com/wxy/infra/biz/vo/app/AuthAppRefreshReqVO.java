package com.wxy.infra.biz.vo.app;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户端续期请求：用登录时返回的续期凭证换取新的一对凭证。
 *
 * <p>与 admin 端的续期入参字段相同但独立存在：两端各自演进时不会互相牵连，
 * 且用户端凭证只认 {@code user_type=2}，语义上属于不同的入口。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AuthAppRefreshReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 续期凭证（登录接口返回的 refreshToken 原值） */
    @NotBlank(message = "续期凭证不能为空")
    private String refreshToken;
}
