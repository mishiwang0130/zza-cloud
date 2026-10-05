package com.wxy.ai.agent.biz.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理端会话列表行。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "管理端会话")
public class ConversationAdminRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID */
    @Schema(description = "会话 ID")
    private Long id;

    /** 所属用户 ID */
    @Schema(description = "所属用户 ID")
    private Long userId;

    /** 会话标题 */
    @Schema(description = "会话标题")
    private String title;

    /** 消息条数 */
    @Schema(description = "消息条数")
    private Integer messageCount;

    /** 最后一条消息时间 */
    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
