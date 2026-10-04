package com.wxy.rental.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公寓分页查询入参。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApartmentPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓名称，模糊匹配 */
    private String name;

    /** 所在区县 ID，精确匹配 */
    private Long districtId;

    /** 所在市 ID，精确匹配；由 Service 展开成区县 ID 列表后查询 */
    private Long cityId;

    /** 付款方式：1 月付、2 季付、3 半年付、4 年付 */
    private Integer paymentMethod;

    /** 发布状态：0 未发布、1 已发布 */
    private Integer publishStatus;
}
