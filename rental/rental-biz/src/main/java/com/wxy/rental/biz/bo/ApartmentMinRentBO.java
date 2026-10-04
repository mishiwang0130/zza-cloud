package com.wxy.rental.biz.bo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公寓最低租金查询结果：SQL 聚合出来的「公寓 ID + 已发布房间最低租金」。
 *
 * <p>单独用一个 BO 接聚合结果，而不是把聚合字段塞进 {@code RentalApartment} 实体：实体与表一一对应，多出来的字段会让 PO 的语义说不清楚。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentMinRentBO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓 ID */
    private Long apartmentId;

    /** 该公寓已发布房间的最低月租金 */
    private BigDecimal minRent;
}
