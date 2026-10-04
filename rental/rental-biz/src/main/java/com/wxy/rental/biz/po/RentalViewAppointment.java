package com.wxy.rental.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 看房预约表 {@code rental_view_appointment} 的实体。
 *
 * <p>姓名与手机是下单时的快照：之后用户改了资料，这条预约仍然要能按当时留的联系方式找人，
 * 所以不做「按 userId 实时查用户表」的展示（那是用户昵称的事，见返回体的说明）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_view_appointment")
public class RentalViewAppointment extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 预约人 App 用户 ID */
    private Long userId;

    /** 预约公寓 ID */
    private Long apartmentId;

    /** 预约人姓名（下单时快照，便于前台联系） */
    private String name;

    /** 预约人手机号（下单时快照，便于前台联系） */
    private String mobile;

    /** 预约看房时间 */
    private LocalDateTime appointmentTime;

    /** 预约状态：1 待看房、2 已取消、3 已看房，取值见 {@code RentalAppointmentStatusEnum} */
    private Integer status;

    /** 备注 */
    private String remark;
}
