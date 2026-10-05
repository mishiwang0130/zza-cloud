package com.wxy.ai.agent.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能客服会话表 {@code ai_agent_conversation} 的实体。
 *
 * <p>一条会话属于一个小程序用户（{@code user_id}）。多轮上下文不在本表里：
 * 它由 Spring AI 的 Redis 会话记忆按 {@code conversationId} 维护，本表只保存
 * 列表与审计需要的聚合信息（标题、消息条数、最后消息时间）。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_conversation")
public class AiAgentConversation extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属小程序用户 ID */
    private Long userId;

    /** 会话标题：取首条用户问题截断，不调用模型生成 */
    private String title;

    /** 消息条数：用户消息与 AI 消息都计入 */
    private Integer messageCount;

    /** 最后一条消息时间：会话列表按它倒序 */
    private LocalDateTime lastMessageTime;
}
