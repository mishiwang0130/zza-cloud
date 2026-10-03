package com.wxy.infra.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import lombok.Data;

/**
 * 权限校验入参：其他服务调 infra 判断「某用户有没有这些权限」时使用。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class PermissionCheckReqDTO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 登录端类型：1 管理后台、2 用户端，取值见 {@code UserTypeEnum}；可为空表示不限制端 */
    private Integer userType;

    /** 需要判断的权限标识，命中任意一个即算有权限 */
    @NotEmpty(message = "权限标识不能为空")
    private Collection<String> permissions;
}
