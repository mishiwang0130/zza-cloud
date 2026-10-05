package com.wxy.ai.agent.biz.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 小程序端提问入参。
 *
 * <p>{@code conversationId} 为空表示开启新会话，服务端创建后通过 SSE 的 {@code meta} 事件回传；
 * {@code city} 是城市标签，用于把知识库检索限定在「选中城市 + 通用」范围。
 *
 * <p>校验注解同时服务于非流式接口（{@code /chat/ask} 会走 Bean Validation）；
 * 流式接口（{@code /chat/stream}）刻意不挂 {@code @Validated}，由 Service 校验后以
 * SSE {@code error} 事件返回，保证流式响应始终是事件流而不是 JSON。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "智能客服提问入参")
public class ChatStreamReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID：为空表示新会话 */
    @Schema(description = "会话 ID，为空表示开启新会话", example = "1001")
    private Long conversationId;

    /** 用户问题 */
    @Schema(description = "用户问题", example = "两室一厅大概多少钱？")
    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过 2000")
    private String message;

    /** 城市标签：为空表示不按城市过滤知识库 */
    @Schema(description = "城市标签，用于过滤知识库", example = "武汉")
    @Size(max = 32, message = "城市标签长度不能超过 32")
    private String city;
}
