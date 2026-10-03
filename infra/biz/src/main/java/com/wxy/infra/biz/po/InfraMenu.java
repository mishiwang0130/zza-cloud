package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单权限表 {@code infra_menu} 的实体：目录、菜单与按钮共用一张表，用 {@code type} 区分。
 *
 * <p>父子关系靠 {@code parent_id} 维护，顶级节点的父 ID 为 0（见 {@code CommonConstant.ROOT_PARENT_ID}）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_menu")
public class InfraMenu extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 上级菜单 ID，0 表示顶级菜单，索引 {@code idx_infra_menu_parent_id} */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 类型：1 目录、2 菜单、3 按钮，取值见 {@code InfraMenuTypeEnum} */
    private Integer type;

    /** 前端路由地址：目录用绝对路径（如 {@code /system}），菜单用相对路径（如 {@code user}） */
    private String path;

    /** 前端组件路径，按钮与目录为空 */
    private String component;

    /** 权限标识（如 {@code infra:user:create}），按钮必填，目录与菜单可空 */
    private String perms;

    /** 图标名称，前端按名称解析 */
    private String icon;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 是否显示：0 显示、1 隐藏，取值见 {@code CommonStatusEnum} */
    private Integer visible;

    /** 状态：0 启用、1 停用，取值见 {@code CommonStatusEnum} */
    private Integer status;
}
