package com.wxy.rental.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租约分页查询入参。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeasePageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 承租人 App 用户 ID */
    private Long userId;

    /** 签约公寓 ID */
    private Long apartmentId;

    /** 签约房间 ID */
    private Long roomId;

    /** 租约状态：1~7 */
    private Integer status;

    /** 租约来源：1 新签、2 续约 */
    private Integer sourceType;

    /** 租约结束日期下限（含） */
    private LocalDate leaseEndDateStart;

    /** 租约结束日期上限（含） */
    private LocalDate leaseEndDateEnd;
}
