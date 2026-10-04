package com.wxy.rental.biz.mq.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.alibaba.fastjson2.JSON;
import com.wxy.rental.biz.mq.message.RentalBrowseHistoryMsg;
import com.wxy.rental.biz.service.RentalBrowseHistoryService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 浏览记录消费者单元测试：只做消息适配与格式校验，异常一律抛出交给 RocketMQ 重试。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalBrowseHistoryConsumerTest {

    /** 浏览记录写入服务 */
    @Mock
    private RentalBrowseHistoryService rentalBrowseHistoryService;

    /** 被测消费者 */
    private RentalBrowseHistoryConsumer consumer;

    /**
     * 装配被测消费者
     */
    @BeforeEach
    void setUp() {
        consumer = new RentalBrowseHistoryConsumer();
        ReflectionTestUtils.setField(consumer, "rentalBrowseHistoryService", rentalBrowseHistoryService);
    }

    /**
     * 正常消息：连同浏览时间一起交给服务落库
     */
    @Test
    @DisplayName("onMessage：把消息体的用户、房间与浏览时间交给写入服务")
    void onMessageShouldDelegateToService() {
        LocalDateTime browseTime = LocalDateTime.of(2026, 10, 4, 15, 30);
        String message = JSON.toJSONString(new RentalBrowseHistoryMsg(100L, 5L, browseTime));

        consumer.onMessage(message);

        verify(rentalBrowseHistoryService).record(100L, 5L, browseTime);
    }

    /**
     * 消息缺字段时抛出异常且不落库
     */
    @Test
    @DisplayName("onMessage：缺少 userId 时抛异常且不落库")
    void onMessageShouldRejectMissingField() {
        assertThatThrownBy(() -> consumer.onMessage("{\"roomId\":5}"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(rentalBrowseHistoryService);
    }

    /**
     * 消息体不是 JSON 时抛出异常且不落库
     */
    @Test
    @DisplayName("onMessage：非法 JSON 时抛异常且不落库")
    void onMessageShouldRejectInvalidJson() {
        assertThatThrownBy(() -> consumer.onMessage("not-a-json"))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(rentalBrowseHistoryService);
    }
}
