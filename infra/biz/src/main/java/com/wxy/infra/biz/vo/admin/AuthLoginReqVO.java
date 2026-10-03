package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 管理后台登录请求。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class AuthLoginReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64 个字符")
    private String username;

    /** 登录密码（明文传输，由服务端与库里的 BCrypt 哈希比对） */
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64 个字符")
    private String password;
}
