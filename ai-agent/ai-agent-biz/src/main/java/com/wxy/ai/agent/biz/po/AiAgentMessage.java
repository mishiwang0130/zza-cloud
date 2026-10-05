package com.wxy.ai.agent.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能客服消息表 {@code ai_agent_message} 的实体：一次问答存两条（用户 + AI）。
 *
 * <p>本表是「历史与审计」的唯一来源：小程序端回放会话读它，管理端排查读它。
 * 模型的多轮上下文由 Redis 会话记忆维护，两者职责不同、互不覆盖写。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_message")
public class AiAgentMessage extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属会话 ID */
    private Long conversationId;

    /** 所属用户 ID：便于后台按用户维度排查，不需要再 join 会话表 */
    private Long userId;

    /** 发送方：1 用户、2 智能客服，取值见 {@code AiAgentMessageSenderEnum} */
    private Integer senderType;

    /** 消息内容 */
    private String content;

    /** 智能客服消息命中的知识来源（JSON 数组字符串，用 Fastjson2 转换）；用户消息为空 */
    private String sources;

    /** 本次回答使用的模型；用户消息为空 */
    private String model;

    /** 本次回答耗时（毫秒）；用户消息为空 */
    private Integer latencyMs;
}
