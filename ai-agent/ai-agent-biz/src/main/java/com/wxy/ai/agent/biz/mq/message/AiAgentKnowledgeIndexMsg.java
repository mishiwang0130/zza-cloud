package com.wxy.ai.agent.biz.mq.message;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库索引任务消息体：上传 / 重建索引接口只发这两个字段。
 *
 * <p>字段刻意只有两个：文件名、城市、存储对象名这些都在 {@code ai_agent_knowledge_document}
 * 里，消费者按 ID 自己查库就行。消息带得越多，越容易出现「消息里的旧值与库里的新值不一致」，
 * 而这类不一致排查起来最费时间。
 *
 * <p>消息体用 Fastjson2 转成 JSON 字符串收发（见 {@code AiAgentMqConstant}），
 * 不用 RocketMQ 默认的 Jackson 转换器，与「业务代码 JSON 统一用 Fastjson2」的规范一致。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentKnowledgeIndexMsg implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识库文档 ID */
    private Long documentId;

    /** 动作：{@code AiAgentKnowledgeIndexActionEnum} 的枚举名 */
    private String action;
}
