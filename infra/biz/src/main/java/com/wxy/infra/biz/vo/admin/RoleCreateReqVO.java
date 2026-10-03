package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 新增角色的请求：创建时可直接勾选菜单与按钮权限。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class RoleCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过 64 个字符")
    private String name;

    /** 角色编码，唯一，建议英文小写下划线（如 {@code infra_admin}） */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不能超过 64 个字符")
    private String code;

    /** 排序号，越小越靠前，不传按 0 处理 */
    private Integer sort;

    /** 状态：0 启用、1 停用，不传按启用处理 */
    private Integer status;

    /** 分配的菜单 ID 列表（含目录、菜单与按钮），可以为空 */
    private List<Long> menuIds;
}
