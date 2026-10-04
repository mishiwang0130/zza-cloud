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
 * 公寓修改入参：{@code id} + 新增接口的全部字段。
 *
 * <p>字段为 null 表示本次不改动（与 MyBatis-Plus 的更新策略一致）：表单少传一个字段不应该等于「把它清空」；要清空标签 / 费用项 / 图片请显式提交空列表。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ApartmentUpdateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 公寓 ID，必填 */
    @NotNull(message = "公寓 ID 不能为空")
    private Long id;

    /** 公寓名称 */
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

    /** 最低起租月数 */
    @Min(value = 1, message = "最低起租月数至少为 1")
    private Integer minLeaseMonths;

    /** 押金月数 */
    @Min(value = 0, message = "押金月数不能为负数")
    private Integer depositMonths;

    /** 付款方式：1 月付、2 季付、3 半年付、4 年付 */
    @Min(value = 1, message = "付款方式不合法")
    @Max(value = 4, message = "付款方式不合法")
    private Integer paymentMethod;

    /** 公寓标签编码列表；不传表示不改动，传空列表表示清空 */
    private List<String> labelCodes;

    /** 公寓配套编码列表；不传表示不改动，传空列表表示清空 */
    private List<String> facilityCodes;

    /** 公寓费用项 ID 列表；不传表示不改动，传空列表表示清空 */
    private List<Long> feeItemIds;

    /** 公寓图片列表；不传表示不改动，传空列表表示清空 */
    @Valid
    private List<ImageItemReqVO> images;
}
