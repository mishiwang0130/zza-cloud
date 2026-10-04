package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 看房预约返回体：列表与详情弹窗共用，所以不单独出详情接口。
 *
 * <p>昵称与手机由 Service 按 {@code userId} 调 infra 的用户接口回填：预约表只存 ID，
 * 用户改了资料，后台看到的就是最新值；用户已删除或查不到时返回 null，不影响这条预约本身。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ViewAppointmentRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 预约 ID */
    private Long id;

    /** 预约人 App 用户 ID */
    private Long userId;

    /** 预约人昵称 */
    private String userNickname;

    /** 预约人手机号 */
    private String userMobile;

    /** 预约公寓 ID */
    private Long apartmentId;

    /** 预约公寓名称，由 Service 按公寓 ID 回填 */
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
