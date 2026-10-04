package com.wxy.rental.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房间分页查询入参。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoomPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属公寓 ID，精确匹配 */
    private Long apartmentId;

    /** 房间号，模糊匹配 */
    private String roomNumber;

    /** 发布状态：0 未发布、1 已发布 */
    private Integer publishStatus;

    /** 月租金下限（含） */
    private BigDecimal minRent;

    /** 月租金上限（含） */
    private BigDecimal maxRent;

    /** 只看空置房间：true 表示排除有生效中租约的房间 */
    private Boolean vacantOnly;
}
