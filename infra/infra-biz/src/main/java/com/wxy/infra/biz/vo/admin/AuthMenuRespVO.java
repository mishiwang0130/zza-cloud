package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 当前用户可见的菜单树节点：只含目录与菜单，按钮不进导航。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class AuthMenuRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 菜单 ID */
    private Long id;

    /** 上级菜单 ID，0 表示顶级 */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 类型：1 目录、2 菜单，取值见 {@code InfraMenuTypeEnum} */
    private Integer type;

    /** 前端路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限标识 */
    private String perms;

    /** 图标名称 */
    private String icon;

    /** 排序号 */
    private Integer sort;

    /** 子菜单，叶子节点为空列表，避免前端拿到 null */
    private List<AuthMenuRespVO> children;
}
