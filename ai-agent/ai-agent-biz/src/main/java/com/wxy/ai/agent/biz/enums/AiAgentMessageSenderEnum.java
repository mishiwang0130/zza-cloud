package com.wxy.ai.agent.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话消息发送方：对应 {@code ai_agent_message.sender_type}。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Getter
@AllArgsConstructor
public enum AiAgentMessageSenderEnum {

    /** 用户（小程序） */
    USER(1, "用户"),

    /** 智能客服（大模型） */
    AI(2, "智能客服");

    /** 入库值 */
    private final Integer value;

    /** 中文描述 */
    private final String label;

    /**
     * 按值查找枚举
     *
     * @param value 入库值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static AiAgentMessageSenderEnum of(Integer value) {
        for (AiAgentMessageSenderEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
