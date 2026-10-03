package com.wxy.infra.biz.vo.admin;

import com.wxy.common.webmvc.validation.Mobile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 新增管理后台用户的请求。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class UserCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录用户名，唯一，创建后不可修改 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度需为 3~64 个字符")
    private String username;

    /** 初始密码，明文入参，服务端落库前转成 BCrypt 哈希 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6~32 个字符")
    private String password;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    /** 手机号，唯一 */
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    /** 状态：0 启用、1 停用；不传按启用处理 */
    private Integer status;

    /** 分配的角色 ID 列表，可以为空表示暂不授权 */
    private List<Long> roleIds;
}
