package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 费用项精简返回体：嵌在公寓详情里的费用项。
 *
 * <p>与 {@code FeeItemRespVO} 字段相同但语义不同：这里表示「某个公寓挂了哪些费用」，
 * 公寓详情不需要费用项的管理端信息，将来给公寓详情加字段时也不会意外影响费用项接口。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class FeeItemSimpleRespVO implements Serializable {

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
