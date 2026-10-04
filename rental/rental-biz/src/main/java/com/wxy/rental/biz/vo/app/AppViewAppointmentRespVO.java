package com.wxy.rental.biz.vo.app;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * App 看房预约返回体：列表与详情共用（列表已经带齐要展示的字段，所以不出单独的详情接口）。
 *
 * <p>不回传手机号：本人提交的预约，展示自己的手机号没有意义（前端已经知道），少返一个字段就少一处敏感信息面。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppViewAppointmentRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 预约 ID */
    private Long id;

    /** 预约公寓 ID */
    private Long apartmentId;

    /** 预约公寓名称，由 Service 批量回填 */
    private String apartmentName;

    /** 预约看房时间 */
    private LocalDateTime appointmentTime;

    /** 预约状态：1 待看房、2 已取消、3 已看房 */
    private Integer status;

    /** 预约状态中文名 */
    private String statusName;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
