package com.wxy.infra.biz.vo.internal;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务内部接口：校验访问令牌的返回体。
 *
 * <p>只返回身份三要素：调用方拿到后放进自己的登录上下文即可，凭证细节（过期时间等）不外泄。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenCheckRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 登录端类型：1 管理后台、2 用户端 */
    private Integer userType;

    /** 登录用户名，仅用于日志与审计展示 */
    private String username;
}
