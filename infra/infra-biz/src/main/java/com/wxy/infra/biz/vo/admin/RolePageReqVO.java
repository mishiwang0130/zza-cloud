package com.wxy.infra.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询入参。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RolePageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色名称，前后模糊匹配 */
    private String name;

    /** 角色编码，前后模糊匹配 */
    private String code;

    /** 状态：0 启用、1 停用，为空表示全部 */
    private Integer status;
}
