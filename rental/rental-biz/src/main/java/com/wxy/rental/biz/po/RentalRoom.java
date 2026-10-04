package com.wxy.rental.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房间表 {@code rental_room} 的实体。
 *
 * <p>房间没有「是否在租」这个字段：它随租约变化，落库就要靠定时任务或事件同步，容易不一致，
 * 因此列表查询时按租约的生效状态现算（见 {@code RentalRoomCheckInStatusEnum}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_room")
public class RentalRoom extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属公寓 ID，索引 {@code idx_rental_room_...} 由唯一键 {@code uk_rental_room_apartment_id_room_number} 覆盖 */
    private Long apartmentId;

    /** 房间号，同一公寓内唯一 */
    private String roomNumber;

    /** 月租金（元/月） */
    private BigDecimal rent;

    /** 面积（㎡） */
    private BigDecimal area;

    /** 户型室数：1 一室、2 两室 */
    private Integer roomCount;

    /** 朝向，取 infra 字典 {@code rental_room_orientation} 的编码 */
    private String orientation;

    /** 楼层，如 3、3/18 */
    private String floorNo;

    /** 发布状态：0 未发布、1 已发布，取值见 {@code RentalPublishStatusEnum}；没有删除接口，下架即置 0 */
    private Integer publishStatus;

    /** 标签编码，逗号分隔，取 infra 字典 {@code rental_room_label} */
    private String labelCodes;

    /** 配套编码，逗号分隔，取 infra 字典 {@code rental_room_facility} */
    private String facilityCodes;
}
