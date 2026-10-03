package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 当前登录用户信息：登录后前端用它渲染用户信息、按钮权限与角色标识。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class AuthUserInfoRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 登录用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 登录端类型：1 管理后台、2 用户端，取值见 {@code UserTypeEnum} */
    private Integer userType;

    /** 角色编码集合，前端据此做粗粒度判断 */
    private List<String> roleCodes;

    /** 权限标识集合，前端据此控制按钮显隐 */
    private List<String> perms;
}
