package com.wxy.infra.biz.vo;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录与续期的凭证返回体：access token 用于访问接口，refresh token 用于过期后换新。
 *
 * <p>放在 {@code vo} 包的根下而不是 {@code vo/admin}：admin 端与 app 端签发的凭证结构完全相同，
 * 它不属于任何一端（端类型只体现在 token 载荷与 {@code infra_token.user_type} 上），
 * 放进某一端的包会让另一端被迫依赖别人的包。
 *
 * @author wxy
 * @date 2026/10/04
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
