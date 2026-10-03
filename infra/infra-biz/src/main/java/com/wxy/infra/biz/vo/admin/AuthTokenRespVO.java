package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录与续期的凭证返回体：access token 用于访问接口，refresh token 用于过期后换新。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 访问凭证：JWT，请求时放在 {@code Authorization} 头里 */
    private String accessToken;

    /** 凭证类型，固定 {@code Bearer}，前端拼请求头时用 */
    private String tokenType;

    /** access token 有效期（秒），前端据此判断是否需要提前续期 */
    private Long expiresIn;

    /** 续期凭证：不透明随机串，只能用于续期接口 */
    private String refreshToken;
}
