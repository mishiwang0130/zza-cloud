package com.wxy.ai.agent.biz.mq.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson2.JSON;
import com.wxy.ai.agent.biz.enums.AiAgentKnowledgeIndexActionEnum;
import com.wxy.ai.agent.biz.mq.message.AiAgentKnowledgeIndexMsg;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 索引任务生产者单元测试：消息按 topic:tag 发 Fastjson2 JSON；投递失败必须抛给调用方。
 *
 * <p>与 rental 的浏览记录生产者相反，这里失败不能被吞掉：消息没投出去，文档就会一直停在「待索引」，
 * 接口必须报错、状态必须落到「索引失败」，用户才知道要重建。
 *
 * @author wxy
 * @date 2026/10/05
 */
@ExtendWith(MockitoExtension.class)
class AiAgentKnowledgeIndexProducerTest {

    /** RocketMQ 模板提供者 */
    @Mock
    private ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    /** RocketMQ 模板 */
    @Mock
    private RocketMQTemplate rocketMQTemplate;

    /** 被测生产者 */
    private AiAgentKnowledgeIndexProducer producer;

    /**
     * 装配被测生产者
     */
    @BeforeEach
    void setUp() {
        producer = new AiAgentKnowledgeIndexProducer();
        ReflectionTestUtils.setField(producer, "rocketMQTemplateProvider", rocketMQTemplateProvider);
    }

    /**
     * 缺少文档 ID 或动作属于编码错误，直接拒绝，不去取模板
     */
    @Test
    @DisplayName("send：参数缺失时直接拒绝")
    void sendShouldRejectMissingArguments() {
        assertThatThrownBy(() -> producer.send(null, AiAgentKnowledgeIndexActionEnum.UPLOAD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> producer.send(1L, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(rocketMQTemplateProvider);
    }

    /**
     * 没配 RocketMQ（缺少 name-server 或 producer.group）时明确报错，不能静默跳过
     */
    @Test
    @DisplayName("send：未配置 RocketMQ 时报错")
    void sendShouldFailWhenTemplateMissing() {
        when(rocketMQTemplateProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> producer.send(1L, AiAgentKnowledgeIndexActionEnum.UPLOAD))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * 发送失败必须向上抛，由服务把文档置成「索引失败」
     */
    @Test
    @DisplayName("send：发送失败向上抛")
    void sendShouldPropagateSendFailure() {
        when(rocketMQTemplateProvider.getIfAvailable()).thenReturn(rocketMQTemplate);
        doThrow(new RuntimeException("mq down")).when(rocketMQTemplate).syncSend(anyString(), anyString());

        assertThatThrownBy(() -> producer.send(1L, AiAgentKnowledgeIndexActionEnum.REBUILD))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mq down");
    }

    /**
     * 正常发送：目的地为 topic:tag，消息体是 Fastjson2 序列化的 JSON
     */
    @Test
    @DisplayName("send：按 topic:tag 发送 Fastjson2 JSON 消息体")
    void sendShouldPublishJsonPayload() {
        when(rocketMQTemplateProvider.getIfAvailable()).thenReturn(rocketMQTemplate);

        producer.send(7L, AiAgentKnowledgeIndexActionEnum.REBUILD);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).syncSend(destinationCaptor.capture(), payloadCaptor.capture());
        assertThat(destinationCaptor.getValue()).isEqualTo("zza-ai-agent-knowledge-index:knowledge-index");
        AiAgentKnowledgeIndexMsg msg = JSON.parseObject(payloadCaptor.getValue(), AiAgentKnowledgeIndexMsg.class);
        assertThat(msg.getDocumentId()).isEqualTo(7L);
        assertThat(msg.getAction()).isEqualTo("REBUILD");
    }
}
