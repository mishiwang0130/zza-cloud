package com.wxy.rental.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租约表 {@code rental_lease} 的实体。
 *
 * <p>租金与押金在签约时从公寓 / 房间快照下来：之后调整房间租金不能影响已签合同，
 * 否则同一份合同前后查出来的金额会不一样。租客实名信息不进本表，只在合同文件里。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_lease")
public class RentalLease extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 承租人 App 用户 ID（infra App 端用户表） */
    private Long userId;

    /** 签约公寓 ID */
    private Long apartmentId;

    /** 签约房间 ID */
    private Long roomId;

    /** 合同文件 ID（infra 文件表），0 表示尚未上传 */
    private Long contractFileId;

    /** 租约开始日期 */
    private LocalDate leaseStartDate;

    /** 租约结束日期：确认退租时改写为实际退租日期，所以提前退租后这里就是真实的租期结束日 */
    private LocalDate leaseEndDate;

    /** 签约月租金（元/月），签约时从房间快照 */
    private BigDecimal rent;

    /** 押金（元），不传时按 租金 × 公寓押金月数 计算 */
    private BigDecimal deposit;

    /** 租约状态：1 签约待确认、2 已签约、3 已取消、4 已到期、5 退租待确认、6 已退租、7 续约待确认 */
    private Integer status;

    /** 租约来源：1 新签、2 续约，取值见 {@code RentalLeaseSourceTypeEnum} */
    private Integer sourceType;

    /** 备注 */
    private String remark;
}
