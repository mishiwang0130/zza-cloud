package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 房间列表行返回体：只带列表展示需要的字段。
 *
 * <p>{@code checkInStatus}（0 空置、1 在租）由租约表的生效状态派生，是本列表必需字段；租约明细不在这里返回，要看租约请走租约列表。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class RoomPageItemRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 房间 ID */
    private Long id;

    /** 所属公寓 ID */
    private Long apartmentId;

    /** 所属公寓名称 */
    private String apartmentName;

    /** 房间号 */
    private String roomNumber;

    /** 月租金（元/月） */
    private BigDecimal rent;

    /** 面积（㎡） */
    private BigDecimal area;

    /** 户型室数：1 一室、2 两室 */
    private Integer roomCount;

    /** 朝向编码 */
    private String orientation;

    /** 朝向中文名，由 Service 查 infra 字典回填 */
    private String orientationName;

    /** 楼层，如 3、3/18 */
    private String floorNo;

    /** 发布状态：0 未发布、1 已发布 */
    private Integer publishStatus;

    /** 入住状态：0 空置、1 在租，由租约表派生 */
    private Integer checkInStatus;
}
