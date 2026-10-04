package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 看房预约返回体：列表与详情弹窗共用，所以不单独出详情接口。
 *
 * <p>姓名与手机取预约快照（下单当时留下的联系方式）；用户昵称依赖 infra 的 App 用户批量查询，未就绪前为 null。
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

    /** 预约公寓 ID */
    private Long apartmentId;

    /** 预约公寓名称，由 Service 按公寓 ID 回填 */
    private String apartmentName;

    /** 预约人姓名（下单时快照） */
    private String name;

    /** 预约人手机号（下单时快照） */
    private String mobile;

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
