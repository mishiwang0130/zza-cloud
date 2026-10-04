package com.wxy.rental.biz.vo.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 房间新增入参：与公寓一样，标签、配套与图片随表单一次提交。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class RoomCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属公寓 ID，必填 */
    @NotNull(message = "所属公寓不能为空")
    private Long apartmentId;

    /** 房间号，必填，同一公寓内唯一 */
    @NotBlank(message = "房间号不能为空")
    @Size(max = 50, message = "房间号长度不能超过 50")
    private String roomNumber;

    /** 月租金（元/月），必填 */
    @NotNull(message = "月租金不能为空")
    @DecimalMin(value = "0", message = "月租金不能为负数")
    private BigDecimal rent;

    /** 面积（㎡） */
    @DecimalMin(value = "0", message = "面积不能为负数")
    private BigDecimal area;

    /** 户型室数：1 一室、2 两室；不传按 1 处理 */
    @Min(value = 1, message = "户型室数至少为 1")
    @Max(value = 9, message = "户型室数不合法")
    private Integer roomCount;

    /** 朝向编码，取 infra 字典 rental_room_orientation */
    @Size(max = 32, message = "朝向长度不能超过 32")
    private String orientation;

    /** 楼层，如 3、3/18 */
    @Size(max = 32, message = "楼层长度不能超过 32")
    private String floorNo;

    /** 房间标签编码列表，取 infra 字典 rental_room_label */
    private List<String> labelCodes;

    /** 房间配套编码列表，取 infra 字典 rental_room_facility */
    private List<String> facilityCodes;

    /** 房间图片列表 */
    @Valid
    private List<ImageItemReqVO> images;
}
