package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 菜单返回体：既是列表元素也是树节点，{@code children} 由服务端用 {@code TreeUtil} 组装。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class MenuRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 菜单 ID */
    private Long id;

    /** 上级菜单 ID，0 表示顶级 */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 类型：1 目录、2 菜单、3 按钮 */
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

    /** 是否显示：0 显示、1 隐藏 */
    private Integer visible;

    /** 状态：0 启用、1 停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 子节点，叶子节点为空列表 */
    private List<MenuRespVO> children;
}
