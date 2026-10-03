package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 管理员重置他人密码的请求：不需要原密码，但受超管保护规则限制。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class UserResetPasswordReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 目标用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    /** 新密码，长度 6~32 位 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需为 6~32 个字符")
    private String newPassword;
}
