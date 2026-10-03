package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 角色返回体：带上已分配的菜单 ID，便于编辑弹窗回显权限树。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class RoleRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    private Long id;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 状态：0 启用、1 停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 已分配的菜单 ID 列表 */
    private List<Long> menuIds;
}
