package com.wxy.rental.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 费用项表 {@code rental_fee_item} 的实体：水费、电费、宽带费这类杂费的定义。
 *
 * <p>费用项是独立维护的小配置表，公寓通过 {@code rental_apartment_fee} 引用它，
 * 所以改一个费用项的金额会同时影响所有引用它的公寓——这是刻意的：金额属于费用项本身，
 * 不属于某个公寓，否则每加一个公寓都要把价格抄一遍。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_fee_item")
public class RentalFeeItem extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 费用项名称，如 水费、电费、宽带费；唯一索引 {@code uk_rental_fee_item_name} */
    private String name;

    /** 费用金额（元） */
    private BigDecimal amount;

    /** 计价单位，如 吨、度、月；无单位时为空串 */
    private String unit;
}
