package com.wxy.ai.agent.biz.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 删除（逻辑删除）我的会话入参。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "删除会话入参")
public class ConversationDeleteReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID */
    @Schema(description = "会话 ID")
    @NotNull(message = "会话 ID 不能为空")
    private Long conversationId;
}
