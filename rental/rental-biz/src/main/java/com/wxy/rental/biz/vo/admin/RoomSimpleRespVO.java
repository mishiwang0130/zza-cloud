package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 房间精简返回体：租约表单选房下拉用。
 *
 * <p>带上发布状态是为了让下拉能标注「未发布」的房间，避免运营给未发布的房间签租约时看不出问题。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class RoomSimpleRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 房间 ID */
    private Long id;

    /** 房间号 */
    private String roomNumber;

    /** 月租金（元/月） */
    private BigDecimal rent;

    /** 发布状态：0 未发布、1 已发布 */
    private Integer publishStatus;
}
