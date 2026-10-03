package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色表 {@code infra_role} 的实体：角色是「用户」与「菜单权限」之间的桥梁。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_role")
public class InfraRole extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色名称，展示用 */
    private String name;

    /** 角色编码，唯一索引 {@code uk_infra_role_code}；超级管理员固定 {@code super_admin}，不允许改 */
    private String code;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 状态：0 启用、1 停用，取值见 {@code CommonStatusEnum} */
    private Integer status;
}
