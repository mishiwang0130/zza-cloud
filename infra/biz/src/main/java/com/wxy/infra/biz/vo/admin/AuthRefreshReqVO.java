package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 续期请求：用登录时返回的续期凭证换取新的一对凭证。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class AuthRefreshReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 续期凭证（登录接口返回的 refreshToken 原值） */
    @NotBlank(message = "续期凭证不能为空")
    private String refreshToken;
}
