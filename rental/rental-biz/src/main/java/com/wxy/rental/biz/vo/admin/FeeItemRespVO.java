package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 费用项返回体：列表与编辑弹窗共用（字段少，不单独出详情接口）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class FeeItemRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 费用项 ID */
    private Long id;

    /** 费用项名称 */
    private String name;

    /** 费用金额（元） */
    private BigDecimal amount;

    /** 计价单位，如 吨、度、月；无单位时为空串 */
    private String unit;
}
