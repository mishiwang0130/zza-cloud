package com.wxy.ai.agent.biz.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.wxy.ai.agent.biz.constant.AiAgentMqConstant;
import com.wxy.ai.agent.biz.enums.AiAgentKnowledgeIndexActionEnum;
import com.wxy.ai.agent.biz.mq.message.AiAgentKnowledgeIndexMsg;
import com.wxy.ai.agent.biz.service.KnowledgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 知识库索引任务消费者：把「解析切片 → 向量入库」从接口里挪到这里执行。
 *
 * <p>业务动作全部交给 {@link KnowledgeService#executeIndexTask}：加文档锁、置状态、删旧向量、读原文件、
 * 解析入库都在服务里，这里只做「消息 → 参数」的适配与格式校验，和 Controller 的职责一样薄。
 *
 * <p><b>异常一律抛出</b>：解析不出消息体、字段缺失、文档正被删除 / 重建占用都直接抛，
 * 让 RocketMQ 走失败重试、超过重试次数进死信队列。文档不存在（已删除）是正常终态，
 * 由服务记一条 warn 返回，不重试。
 *
 * <p>{@code parse-mode=sync} 时这个 Bean 不注册：本地没起 RocketMQ 时退回接口内同步解析，
 * 既不订阅 topic，也不会因为连不上 NameServer 而影响启动。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "zza.ai-agent.knowledge.parse-mode", havingValue = "mq", matchIfMissing = true)
@RocketMQMessageListener(topic = AiAgentMqConstant.KNOWLEDGE_INDEX_TOPIC,
        selectorExpression = AiAgentMqConstant.KNOWLEDGE_INDEX_TAG,
        consumerGroup = AiAgentMqConstant.KNOWLEDGE_INDEX_CONSUMER_GROUP)
public class AiAgentKnowledgeIndexConsumer implements RocketMQListener<String> {

    /** 知识库服务：解析入库的实际动作都在它里面 */
    @Resource
    private KnowledgeService knowledgeService;

    /**
     * 消费索引任务消息
     *
     * @param message 消息体（Fastjson2 序列化的 JSON 字符串）
     */
    @Override
    public void onMessage(String message) {
        AiAgentKnowledgeIndexMsg msg = parseMessage(message);
        AiAgentKnowledgeIndexActionEnum action = AiAgentKnowledgeIndexActionEnum.ofName(msg.getAction());
        if (action == null) {
            // 动作不认识说明生产端与消费端版本不一致，重试也救不回来，仍然抛出进死信队列等人工处理
            log.error("[onMessage][索引任务动作无法识别] message={}", message);
            throw new IllegalArgumentException("索引任务动作无法识别：" + msg.getAction());
        }
        knowledgeService.executeIndexTask(msg.getDocumentId(), action);
        log.info("[onMessage][索引任务已处理] documentId={}, action={}", msg.getDocumentId(), action.getLabel());
    }

    /**
     * 解析并校验消息体
     *
     * @param message 原始消息
     * @return 消息体
     * @throws IllegalArgumentException 消息缺少必填字段
     */
    private AiAgentKnowledgeIndexMsg parseMessage(String message) {
        AiAgentKnowledgeIndexMsg msg;
        try {
            msg = JSON.parseObject(message, AiAgentKnowledgeIndexMsg.class);
        } catch (RuntimeException ex) {
            log.error("[parseMessage][索引任务消息解析失败] message={}", message, ex);
            throw ex;
        }
        if (msg == null || msg.getDocumentId() == null || msg.getAction() == null) {
            log.error("[parseMessage][索引任务消息缺少必填字段] message={}", message);
            throw new IllegalArgumentException("索引任务消息缺少 documentId 或 action：" + message);
        }
        return msg;
    }
}
