package com.wxy.ai.agent.biz.mq.producer;

import com.alibaba.fastjson2.JSON;
import com.wxy.ai.agent.biz.constant.AiAgentMqConstant;
import com.wxy.ai.agent.biz.enums.AiAgentKnowledgeIndexActionEnum;
import com.wxy.ai.agent.biz.mq.message.AiAgentKnowledgeIndexMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 知识库索引任务生产者：上传 / 重建索引接口把「解析入库」这件事甩给它，接口立刻返回。
 *
 * <p><b>与 rental 的浏览记录生产者相反，这里的发送失败必须抛给调用方</b>：浏览记录是顺带产生的数据，
 * 发不出去也不影响主流程；而索引任务是用户提交的写操作，消息投不出去这篇文档就永远停在「待索引」，
 * 必须让接口报错、让文档状态落到「索引失败」，用户才知道要重试。
 *
 * <p>消息体统一用 Fastjson2 转成 JSON 字符串发送，不用 RocketMQ 默认的消息转换器：
 * 这样消息在控制台里可以直接读，格式也只由 ai-agent 自己决定，不会因为换个转换器就变形状。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Component
public class AiAgentKnowledgeIndexProducer {

    /**
     * RocketMQ 模板：用 {@link ObjectProvider} 而不是直接注入。
     *
     * <p>没开 MQ 的环境（{@code zza.ai-agent.knowledge.parse-mode=sync}）也应当能启动，
     * 所以这里不在构造期强依赖模板；真到了要发消息却没有模板时再明确报错。
     *
     * <p>模板不存在有两种原因，都要查配置：{@code rocketmq.name-server} 没配；或它与
     * {@code rocketmq.producer.group} 只配了一个——starter 建 DefaultMQProducer 要求两个属性同时存在。
     */
    @Resource
    private ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    /**
     * 投递一条索引任务
     *
     * @param documentId 知识库文档 ID
     * @param action     动作：上传入库 / 重建索引
     * @throws IllegalStateException RocketMQ 未配置（缺少 name-server 或 producer.group）
     * @throws RuntimeException      发送失败（NameServer 不可达、Broker 拒绝等）
     */
    public void send(Long documentId, AiAgentKnowledgeIndexActionEnum action) {
        if (documentId == null || action == null) {
            throw new IllegalArgumentException("索引任务缺少 documentId 或 action");
        }
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            throw new IllegalStateException(
                    "RocketMQTemplate 不存在，检查 rocketmq.name-server 与 rocketmq.producer.group");
        }
        String destination = AiAgentMqConstant.KNOWLEDGE_INDEX_TOPIC + ":" + AiAgentMqConstant.KNOWLEDGE_INDEX_TAG;
        String payload = JSON.toJSONString(new AiAgentKnowledgeIndexMsg(documentId, action.name()));
        try {
            rocketMQTemplate.syncSend(destination, payload);
        } catch (RuntimeException ex) {
            log.error("[send][索引任务投递失败] documentId={}, action={}, error={}",
                    documentId, action, ex.getMessage(), ex);
            throw ex;
        }
        log.info("[send][索引任务已投递] documentId={}, action={}", documentId, action.getLabel());
    }
}
