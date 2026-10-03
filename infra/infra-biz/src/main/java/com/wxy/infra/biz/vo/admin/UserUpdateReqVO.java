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
 * 修改管理后台用户的请求：用户名不可改（它是登录标识，改了会让既有操作记录对不上人）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class UserUpdateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    /** 手机号，唯一 */
    @NotBlank(message = "手机号不能为空")
    @Mobile
    private String mobile;

    /** 状态：0 启用、1 停用；不传表示不变 */
    private Integer status;

    /** 分配的角色 ID 列表，传空列表表示清空角色 */
    private List<Long> roleIds;
}
