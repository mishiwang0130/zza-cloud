package com.wxy.rental.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 看房预约分页查询入参。
 *
 * <p>没有姓名与手机号筛选：这两项不落在预约表里，按它们过滤要先反查用户表，
 * 而 infra 只提供按 ID 批量查（不提供按手机号搜索），所以这里只能按 {@code userId} 精确定位。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ViewAppointmentPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 预约人 App 用户 ID */
    private Long userId;

    /** 预约公寓 ID */
    private Long apartmentId;

    /** 预约状态：1 待看房、2 已取消、3 已看房 */
    private Integer status;

    /** 预约看房时间下限（含） */
    private LocalDateTime appointmentTimeStart;

    /** 预约看房时间上限（含） */
    private LocalDateTime appointmentTimeEnd;
}
