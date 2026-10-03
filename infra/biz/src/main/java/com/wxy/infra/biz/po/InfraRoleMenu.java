package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色与菜单的关联表 {@code infra_role_menu} 的实体：一个角色可以分配多个菜单与按钮权限。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_role_menu")
public class InfraRoleMenu extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    private Long roleId;

    /** 菜单 ID */
    private Long menuId;
}
