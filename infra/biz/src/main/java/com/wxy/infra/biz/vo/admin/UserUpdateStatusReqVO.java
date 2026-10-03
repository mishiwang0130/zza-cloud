package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 修改用户状态（启用 / 停用）的请求。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class UserUpdateStatusReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    /** 目标状态：0 启用、1 停用，取值见 {@code CommonStatusEnum} */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
