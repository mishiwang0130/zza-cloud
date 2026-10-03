package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 角色精简返回体：用户编辑弹窗里的角色下拉用，只给必要字段。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class RoleSimpleRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    private Long id;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;
}
