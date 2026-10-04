package com.wxy.rental.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公寓表 {@code rental_apartment} 的实体。
 *
 * <p>标签与配套按逗号分隔的编码存在主表上（{@code label_codes} / {@code facility_codes}）：
 * 它们是纯展示的多选标记，不需要独立查询，拆关系表只会多两次 join；反过来费用项与图片是
 * 独立实体（费用项能被多个公寓引用、图片要单独排序），所以走关系表。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_apartment")
public class RentalApartment extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓名称 */
    private String name;

    /** 公寓介绍 */
    private String introduction;

    /** 所在区县 ID（infra 行政区划表的区县节点），省 / 市由父级链推导 */
    private Long districtId;

    /** 详细地址（不含省市区前缀） */
    private String addressDetail;

    /** 公寓前台电话 */
    private String phone;

    /** 最低起租月数，更长的租期自然支持 */
    private Integer minLeaseMonths;

    /** 押金月数，如 1 表示押一个月 */
    private Integer depositMonths;

    /** 付款方式：1 月付、2 季付、3 半年付、4 年付，取值见 {@code RentalPaymentMethodEnum} */
    private Integer paymentMethod;

    /** 发布状态：0 未发布、1 已发布，取值见 {@code RentalPublishStatusEnum}；没有删除接口，下架即置 0 */
    private Integer publishStatus;

    /** 标签编码，逗号分隔，取 infra 字典 {@code rental_apartment_label} */
    private String labelCodes;

    /** 配套编码，逗号分隔，取 infra 字典 {@code rental_apartment_facility} */
    private String facilityCodes;
}
