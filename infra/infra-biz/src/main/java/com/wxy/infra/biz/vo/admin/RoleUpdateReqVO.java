package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 修改角色的请求：菜单权限以提交的 menuIds 为准做全量覆盖。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class RoleUpdateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    @NotNull(message = "角色 ID 不能为空")
    private Long id;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过 64 个字符")
    private String name;

    /** 角色编码；超级管理员角色的编码不允许修改 */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不能超过 64 个字符")
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 状态：0 启用、1 停用 */
    private Integer status;

    /** 分配的菜单 ID 列表，传空列表表示清空权限 */
    private List<Long> menuIds;
}
