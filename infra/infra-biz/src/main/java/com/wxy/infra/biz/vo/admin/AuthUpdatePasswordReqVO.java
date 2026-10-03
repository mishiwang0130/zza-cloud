package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 修改当前登录用户密码的请求。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class AuthUpdatePasswordReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 原密码：必须校验通过才允许修改，防止凭证被盗后直接改密码 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 新密码，长度 6~32 位 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需为 6~32 个字符")
    private String newPassword;
}
