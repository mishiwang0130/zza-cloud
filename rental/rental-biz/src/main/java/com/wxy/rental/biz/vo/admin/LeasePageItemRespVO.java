package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 租约列表行返回体：只带列表展示需要的字段。
 *
 * <p>不含合同文件与备注：那是详情与编辑弹窗才需要的内容。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class LeasePageItemRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 租约 ID */
    private Long id;

    /** 承租人 App 用户 ID */
    private Long userId;

    /** 承租人昵称 */
    private String userNickname;

    /** 承租人手机号 */
    private String userMobile;

    /** 签约公寓 ID */
    private Long apartmentId;

    /** 签约公寓名称 */
    private String apartmentName;

    /** 签约房间 ID */
    private Long roomId;

    /** 签约房间号 */
    private String roomNumber;

    /** 租约开始日期 */
    private LocalDate leaseStartDate;

    /** 租约结束日期 */
    private LocalDate leaseEndDate;

    /** 签约月租金（元/月） */
    private BigDecimal rent;

    /** 押金（元） */
    private BigDecimal deposit;

    /** 租约状态：1 签约待确认 ~ 7 续约待确认 */
    private Integer status;

    /** 租约状态中文名 */
    private String statusName;

    /** 租约来源：1 新签、2 续约 */
    private Integer sourceType;

    /** 创建时间 */
    private LocalDateTime createTime;
}
