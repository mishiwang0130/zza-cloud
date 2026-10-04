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
 * <p>只存预约人 ID，不存姓名与手机：联系方式是用户档案的一部分，改了资料就该以最新为准，
 * 快照一份下来既会过期，也把同一份敏感信息复制到了业务库里；后台要展示时按 {@code userId}
 * 调 infra 的用户接口回填（见 {@code RentalAppUserService}）。
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

    /** 预约看房时间 */
    private LocalDateTime appointmentTime;

    /** 预约状态：1 待看房、2 已取消、3 已看房，取值见 {@code RentalAppointmentStatusEnum} */
    private Integer status;

    /** 备注 */
    private String remark;
}
