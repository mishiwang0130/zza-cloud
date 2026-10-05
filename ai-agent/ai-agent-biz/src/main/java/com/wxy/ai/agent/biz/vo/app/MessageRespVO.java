package com.wxy.ai.agent.biz.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 会话中的一条消息：小程序端拉历史时按 id 升序返回。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "会话消息")
public class MessageRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息 ID：客户端用 lastId 做增量拉取 */
    @Schema(description = "消息 ID")
    private Long id;

    /** 发送方：1 用户、2 智能客服，取值见 {@code AiAgentMessageSenderEnum} */
    @Schema(description = "发送方：1 用户、2 智能客服")
    private Integer senderType;

    /** 消息内容 */
    @Schema(description = "消息内容")
    private String content;

    /** 智能客服消息命中的知识来源；用户消息为空列表 */
    @Schema(description = "命中的知识来源")
    private List<ChatSourceRespVO> sources;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
