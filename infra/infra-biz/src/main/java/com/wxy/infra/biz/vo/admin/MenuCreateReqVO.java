package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 新增菜单（目录 / 菜单 / 按钮）的请求。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class MenuCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 上级菜单 ID，0 表示顶级 */
    @NotNull(message = "上级菜单不能为空")
    private Long parentId;

    /** 菜单名称 */
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称长度不能超过 64 个字符")
    private String name;

    /** 类型：1 目录、2 菜单、3 按钮，取值见 {@code InfraMenuTypeEnum} */
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    /** 前端路由地址 */
    @Size(max = 200, message = "路由地址长度不能超过 200 个字符")
    private String path;

    /** 前端组件路径 */
    @Size(max = 200, message = "组件路径长度不能超过 200 个字符")
    private String component;

    /** 权限标识（如 {@code infra:user:create}），按钮必填 */
    @Size(max = 100, message = "权限标识长度不能超过 100 个字符")
    private String perms;

    /** 图标名称 */
    @Size(max = 64, message = "图标名称长度不能超过 64 个字符")
    private String icon;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 是否显示：0 显示、1 隐藏 */
    private Integer visible;

    /** 状态：0 启用、1 停用 */
    private Integer status;
}
