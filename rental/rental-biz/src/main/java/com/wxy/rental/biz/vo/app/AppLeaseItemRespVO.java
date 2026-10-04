package com.wxy.rental.biz.vo.app;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * App 我的租约列表行返回体：只带租约卡片要展示的字段。
 *
 * <p>封面图取房间图片里排序最靠前的一张：租约卡片上放一张房间照片是用户识别「这是哪套房」最直接的方式。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppLeaseItemRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 租约 ID */
    private Long id;

    /** 签约公寓 ID */
    private Long apartmentId;

    /** 签约公寓名称 */
    private String apartmentName;

    /** 签约房间 ID */
    private Long roomId;

    /** 签约房间号 */
    private String roomNumber;

    /** 签约月租金（元/月） */
    private BigDecimal rent;

    /** 押金（元） */
    private BigDecimal deposit;

    /** 租约开始日期 */
    private LocalDate leaseStartDate;

    /** 租约结束日期 */
    private LocalDate leaseEndDate;

    /** 租约状态：1 签约待确认、2 已签约、3 已取消、4 已到期、5 退租待确认、6 已退租、7 续约待确认 */
    private Integer status;

    /** 租约状态中文名 */
    private String statusName;

    /** 租约来源：1 新签、2 续约 */
    private Integer sourceType;

    /** 房间封面图文件 ID，没有图片时为 null */
    private Long coverFileId;
}
