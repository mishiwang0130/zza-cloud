package com.wxy.common.core.context;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户：网关解析凭证后写入请求头，业务服务再放进 {@link UserContextHolder}。
 *
 * <p>它只承载「谁在操作」这一件事，不参与鉴权判断；是否放行由网关与各服务的拦截逻辑决定。
 *
 * @param userId   用户 ID，必填
 * @param userType 登录端类型，取值见 {@code UserTypeEnum}，可以为 null（系统内部调用）
 * @param username 用户名，仅用于日志与审计展示，可以为 null
 * @author wxy
 * @date 2026/10/02
 */
public record LoginUser(Long userId, Integer userType, String username) implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 校验必填字段
     *
     * @param userId 用户 ID，不能为 null
     */
    public LoginUser {
        if (userId == null) {
            throw new IllegalArgumentException("登录用户 ID 不能为空");
        }
    }
}
