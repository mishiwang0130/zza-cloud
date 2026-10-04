package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 公寓列表行返回体：只带列表展示需要的字段。
 *
 * <p>{@code roomCount} / {@code vacantRoomCount} 是本列表唯一的聚合字段（首页看空置用），房间明细走房间接口；列表不带介绍、电话、图片这些详情字段。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ApartmentPageItemRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓 ID */
    private Long id;

    /** 公寓名称 */
    private String name;

    /** 所在区县 ID */
    private Long districtId;

    /** 所在区县名称，由 Service 查 infra 区划回填 */
    private String districtName;

    /** 详细地址 */
    private String addressDetail;

    /** 最低起租月数 */
    private Integer minLeaseMonths;

    /** 押金月数 */
    private Integer depositMonths;

    /** 付款方式：1 月付、2 季付、3 半年付、4 年付 */
    private Integer paymentMethod;

    /** 发布状态：0 未发布、1 已发布 */
    private Integer publishStatus;

    /** 房间总数 */
    private Long roomCount;

    /** 空置房间数（没有生效中租约的房间） */
    private Long vacantRoomCount;
}
