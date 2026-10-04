package com.wxy.rental.biz.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 费用项精简返回体：嵌在公寓详情里的费用项（管理端详情与用户端详情共用）。
 *
 * <p>放在 {@code vo} 根包而不是 {@code vo/admin}：两端展示的都是「这个公寓挂了哪些费用」，
 * 字段完全一致；与 {@code FeeItemRespVO} 的区别是那个是费用项管理接口的返回体，
 * 带的是费用项自身的管理端语义。
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
