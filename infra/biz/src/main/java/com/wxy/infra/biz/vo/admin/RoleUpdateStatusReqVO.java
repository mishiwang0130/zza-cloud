package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 修改角色状态的请求。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class RoleUpdateStatusReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    @NotNull(message = "角色 ID 不能为空")
    private Long id;

    /** 目标状态：0 启用、1 停用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
