package com.wxy.rental.biz.vo.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 房间上架 / 下架入参。
 *
 * <p>房间没有删除接口：下架前要确认房间没有生效中的租约（状态 1/2/5）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class RoomUpdatePublishStatusReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 房间 ID，必填 */
    @NotNull(message = "房间 ID 不能为空")
    private Long id;

    /** 发布状态：0 未发布、1 已发布，必填 */
    @NotNull(message = "发布状态不能为空")
    @Min(value = 0, message = "发布状态不合法")
    @Max(value = 1, message = "发布状态不合法")
    private Integer publishStatus;
}
