package com.wxy.infra.biz.vo.internal;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 服务内部接口：校验访问令牌的请求。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class TokenCheckReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 裸访问令牌（不含 Bearer 前缀） */
    @NotBlank(message = "令牌不能为空")
    private String token;

    /**
     * 期望的登录端类型：1 管理后台、2 用户端，取值见 {@code UserTypeEnum}。
     *
     * <p>可选：传了就要求令牌是该端签发的；不传表示不比对端类型（调用方自己清楚请求属于哪一端）。
     */
    private Integer userType;
}
