package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 租约详情返回体。
 *
 * <p>刻意不含租客实名信息（实名只在合同文件里）与付款方式快照：前者属于敏感信息，
 * 后者会随公寓配置变化，留在租约上只会产生两份互相矛盾的口径。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class LeaseRespVO implements Serializable {

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

    /** 合同文件 ID，0 表示尚未上传 */
    private Long contractFileId;

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

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
