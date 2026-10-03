package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户与角色的关联表 {@code infra_user_role} 的实体：一个用户可以有多个角色。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_user_role")
public class InfraUserRole extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 角色 ID */
    private Long roleId;
}
