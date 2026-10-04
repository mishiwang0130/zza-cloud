package com.wxy.rental.biz.vo.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 公寓新增入参：一次表单提交就落全主表、费用项与图片。
 *
 * <p>标签 / 配套提交的是字典编码列表，费用项与图片提交的是 ID 列表；前端不需要先调「保存图片」之类的第二个接口，Service 在同一个事务里处理完整张表单。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ApartmentCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓名称，必填 */
    @NotBlank(message = "公寓名称不能为空")
    @Size(max = 100, message = "公寓名称长度不能超过 100")
    private String name;

    /** 公寓介绍 */
    @Size(max = 1000, message = "公寓介绍长度不能超过 1000")
    private String introduction;

    /** 所在区县 ID，必填；必须是 infra 行政区划表里真实存在的节点 */
    @NotNull(message = "所在区县不能为空")
    private Long districtId;

    /** 详细地址（不含省市区前缀） */
    @Size(max = 255, message = "详细地址长度不能超过 255")
    private String addressDetail;

    /** 公寓前台电话 */
    @Size(max = 20, message = "联系电话长度不能超过 20")
    private String phone;

    /** 最低起租月数；不传按 1 个月处理 */
    @Min(value = 1, message = "最低起租月数至少为 1")
    private Integer minLeaseMonths;

    /** 押金月数；不传按 1 个月处理，租约押金按它与租金计算 */
    @Min(value = 0, message = "押金月数不能为负数")
    private Integer depositMonths;

    /** 付款方式：1 月付、2 季付、3 半年付、4 年付；不传按月付处理 */
    @Min(value = 1, message = "付款方式不合法")
    @Max(value = 4, message = "付款方式不合法")
    private Integer paymentMethod;

    /** 公寓标签编码列表，取 infra 字典 rental_apartment_label */
    private List<String> labelCodes;

    /** 公寓配套编码列表，取 infra 字典 rental_apartment_facility */
    private List<String> facilityCodes;

    /** 公寓包含的费用项 ID 列表 */
    private List<Long> feeItemIds;

    /** 公寓图片列表 */
    @Valid
    private List<ImageItemReqVO> images;
}
