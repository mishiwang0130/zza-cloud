package com.wxy.rental.biz.vo.app;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * App 公寓列表查询入参：只查已发布公寓，租金 / 面积 / 室数条件落到房间表上过滤。
 *
 * <p>多选标签之间是「或」的关系（命中任意一个即返回）：这是列表筛选的常见口径，用户勾两个标签是「想看有这两个特点的房源」而不是「必须同时具备」。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppApartmentPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所在区县 ID */
    private Long districtId;

    /** 所在市 ID：由 Service 展开成区县 ID 列表后查询 */
    private Long cityId;

    /** 月租金下限（含），按公寓下已发布房间的最低租金判断 */
    private BigDecimal minRent;

    /** 月租金上限（含） */
    private BigDecimal maxRent;

    /** 付款方式：1 月付、2 季付、3 半年付、4 年付 */
    private Integer paymentMethod;

    /** 期望起租月数：返回「最低起租月数不超过它」的公寓（用户能租得起这么久才展示） */
    private Integer minLeaseMonths;

    /** 户型室数：要求公寓下存在该室数的已发布房间 */
    private Integer roomCount;

    /** 面积下限（㎡） */
    private BigDecimal minArea;

    /** 面积上限（㎡） */
    private BigDecimal maxArea;

    /** 公寓标签编码，多选之间是「或」 */
    private List<String> labelCodes;

    /** 关键字：按公寓名称模糊匹配，空白视为不过滤 */
    private String keyword;

    /** 排序方式：0 综合（默认，保持现有排序）、1 月租金从低到高、2 月租金从高到低、3 最新上架；非法值按 0 */
    private Integer sortType;
}
