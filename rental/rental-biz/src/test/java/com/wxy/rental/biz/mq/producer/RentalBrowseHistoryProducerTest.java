package com.wxy.rental.biz.mq.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson2.JSON;
import com.wxy.rental.biz.mq.message.RentalBrowseHistoryMsg;
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
 * 浏览记录生产者单元测试：只在「有用户 + 有 MQ」时发送，任何失败都不得影响调用方。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalBrowseHistoryProducerTest {

    /** RocketMQ 模板提供者 */
    @Mock
    private ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    /** RocketMQ 模板 */
    @Mock
    private RocketMQTemplate rocketMQTemplate;

    /** 被测生产者 */
    private RentalBrowseHistoryProducer producer;

    /**
     * 装配被测生产者
     */
    @BeforeEach
    void setUp() {
        producer = new RentalBrowseHistoryProducer();
        ReflectionTestUtils.setField(producer, "rocketMQTemplateProvider", rocketMQTemplateProvider);
    }

    /**
     * 未登录浏览（userId 为 null）时不发消息，也不去取模板
     */
    @Test
    @DisplayName("send：userId 为空时不发送")
    void sendShouldSkipWhenAnonymous() {
        producer.send(null, 5L);

        verifyNoInteractions(rocketMQTemplateProvider);
    }

    /**
     * 未配置 RocketMQ 时只记日志，不抛异常
     */
    @Test
    @DisplayName("send：未配置 RocketMQ 时只记日志")
    void sendShouldSkipWhenTemplateMissing() {
        when(rocketMQTemplateProvider.getIfAvailable()).thenReturn(null);

        assertThatCode(() -> producer.send(100L, 5L)).doesNotThrowAnyException();
    }

    /**
     * 发送异常时吞成日志，不影响详情接口
     */
    @Test
    @DisplayName("send：发送异常不影响调用方")
    void sendShouldSwallowSendFailure() {
        when(rocketMQTemplateProvider.getIfAvailable()).thenReturn(rocketMQTemplate);
        doThrow(new RuntimeException("mq down")).when(rocketMQTemplate).syncSend(anyString(), anyString());

        assertThatCode(() -> producer.send(100L, 5L)).doesNotThrowAnyException();
    }

    /**
     * 正常发送：目的地为 topic:tag，消息体是 Fastjson2 序列化的 JSON
     */
    @Test
    @DisplayName("send：按 topic:tag 发送 Fastjson2 JSON 消息体")
    void sendShouldPublishJsonPayload() {
        when(rocketMQTemplateProvider.getIfAvailable()).thenReturn(rocketMQTemplate);

        producer.send(100L, 5L);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).syncSend(destinationCaptor.capture(), payloadCaptor.capture());
        assertThat(destinationCaptor.getValue()).isEqualTo("zza-rental-room-browse-history:room-browse");
        RentalBrowseHistoryMsg msg = JSON.parseObject(payloadCaptor.getValue(), RentalBrowseHistoryMsg.class);
        assertThat(msg.getUserId()).isEqualTo(100L);
        assertThat(msg.getRoomId()).isEqualTo(5L);
        assertThat(msg.getBrowseTime()).isNotNull();
    }
}
