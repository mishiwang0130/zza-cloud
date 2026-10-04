package com.wxy.rental.biz.vo.admin;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 费用项删除入参：按接口约定，写接口一律用 POST + 请求体，所以单个 ID 也包成一个对象。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class FeeItemDeleteReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 费用项 ID，必填 */
    @NotNull(message = "费用项 ID 不能为空")
    private Long id;
}
