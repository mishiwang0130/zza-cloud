package com.wxy.ai.agent.biz.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 小程序端「我的会话」列表行：只带会话卡片要展示的字段。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "我的会话")
public class ConversationRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID */
    @Schema(description = "会话 ID")
    private Long id;

    /** 会话标题：取首条用户问题截断 */
    @Schema(description = "会话标题")
    private String title;

    /** 消息条数（用户 + 智能客服） */
    @Schema(description = "消息条数")
    private Integer messageCount;

    /** 最后一条消息时间：列表按它倒序 */
    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
