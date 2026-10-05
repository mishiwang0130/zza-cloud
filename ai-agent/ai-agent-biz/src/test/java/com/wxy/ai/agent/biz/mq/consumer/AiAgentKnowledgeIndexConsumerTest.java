package com.wxy.ai.agent.biz.mq.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.alibaba.fastjson2.JSON;
import com.wxy.ai.agent.biz.enums.AiAgentKnowledgeIndexActionEnum;
import com.wxy.ai.agent.biz.mq.message.AiAgentKnowledgeIndexMsg;
import com.wxy.ai.agent.biz.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 索引任务消费者单元测试：消费者只做「消息 → 参数」的适配与校验，业务动作都在服务里。
 *
 * @author wxy
 * @date 2026/10/05
 */
@ExtendWith(MockitoExtension.class)
class AiAgentKnowledgeIndexConsumerTest {

    /** 知识库服务替身 */
    @Mock
    private KnowledgeService knowledgeService;

    /** 被测消费者 */
    private AiAgentKnowledgeIndexConsumer consumer;

    /**
     * 装配被测消费者
     */
    @BeforeEach
    void setUp() {
        consumer = new AiAgentKnowledgeIndexConsumer();
        ReflectionTestUtils.setField(consumer, "knowledgeService", knowledgeService);
    }

    /**
     * 正常消息：按消息里的文档 ID 与动作调用服务
     */
    @Test
    @DisplayName("onMessage：解析消息并委派给服务")
    void onMessageShouldDelegateToService() {
        String message = JSON.toJSONString(
                new AiAgentKnowledgeIndexMsg(3L, AiAgentKnowledgeIndexActionEnum.UPLOAD.name()));

        consumer.onMessage(message);

        verify(knowledgeService).executeIndexTask(3L, AiAgentKnowledgeIndexActionEnum.UPLOAD);
    }

    /**
     * 缺少必填字段的消息重试也救不回来，抛出交给 RocketMQ 的重试与死信队列，避免静默丢数据
     */
    @Test
    @DisplayName("onMessage：缺少字段时抛出且不调用服务")
    void onMessageShouldRejectMissingFields() {
        assertThatThrownBy(() -> consumer.onMessage("{\"documentId\":3}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.onMessage("{\"action\":\"UPLOAD\"}"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(knowledgeService);
    }

    /**
     * 动作不认识说明生产端与消费端版本不一致，直接抛出，不做「当作上传处理」这类兜底
     */
    @Test
    @DisplayName("onMessage：动作无法识别时抛出")
    void onMessageShouldRejectUnknownAction() {
        assertThatThrownBy(() -> consumer.onMessage("{\"documentId\":3,\"action\":\"REINDEX\"}"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(knowledgeService);
    }

    /**
     * 消息体本身不是 JSON 时抛出解析异常
     */
    @Test
    @DisplayName("onMessage：消息体不是合法 JSON 时抛出")
    void onMessageShouldRejectMalformedPayload() {
        assertThatThrownBy(() -> consumer.onMessage("not-a-json"))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(knowledgeService);
    }
}
