package com.wxy.rental.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公寓费用项关联表 {@code rental_apartment_fee} 的实体。
 *
 * <p>关系表不保留历史：覆盖写时先按公寓 ID 物理删除再整体插入。
 * 逻辑删除会让旧行继续占着 {@code (apartment_id, fee_item_id)} 唯一键，
 * 「先删后插」同一个费用项就会报 Duplicate entry。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_apartment_fee")
public class RentalApartmentFee extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓 ID */
    private Long apartmentId;

    /** 费用项 ID */
    private Long feeItemId;
}
