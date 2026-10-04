package com.wxy.rental.biz.mq.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.alibaba.fastjson2.JSON;
import com.wxy.rental.biz.mapper.RentalBrowseHistoryMapper;
import com.wxy.rental.biz.mq.message.RentalBrowseHistoryMsg;
import com.wxy.rental.biz.po.RentalBrowseHistory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 浏览记录消费者单元测试：纯插入、不去重，异常一律抛出交给 RocketMQ 重试。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalBrowseHistoryConsumerTest {

    /** 浏览记录 Mapper */
    @Mock
    private RentalBrowseHistoryMapper rentalBrowseHistoryMapper;

    /** 被测消费者 */
    private RentalBrowseHistoryConsumer consumer;

    /**
     * 装配被测消费者
     */
    @BeforeEach
    void setUp() {
        consumer = new RentalBrowseHistoryConsumer();
        ReflectionTestUtils.setField(consumer, "rentalBrowseHistoryMapper", rentalBrowseHistoryMapper);
    }

    /**
     * 正常消息：插入一条流水，浏览时间取自消息
     */
    @Test
    @DisplayName("onMessage：按消息时间插入一条浏览流水")
    void onMessageShouldInsertWithMessageTime() {
        LocalDateTime browseTime = LocalDateTime.of(2026, 10, 4, 15, 30);
        String message = JSON.toJSONString(new RentalBrowseHistoryMsg(100L, 5L, browseTime));

        consumer.onMessage(message);

        ArgumentCaptor<RentalBrowseHistory> captor = ArgumentCaptor.forClass(RentalBrowseHistory.class);
        verify(rentalBrowseHistoryMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
        assertThat(captor.getValue().getRoomId()).isEqualTo(5L);
        assertThat(captor.getValue().getCreateTime()).isEqualTo(browseTime);
    }

    /**
     * 同一房间浏览多次就是多条流水：不去重
     */
    @Test
    @DisplayName("onMessage：同一房间多次浏览插入多条流水")
    void onMessageShouldInsertEveryTime() {
        String message = JSON.toJSONString(new RentalBrowseHistoryMsg(100L, 5L, LocalDateTime.now()));

        consumer.onMessage(message);
        consumer.onMessage(message);

        verify(rentalBrowseHistoryMapper, times(2)).insert(any(RentalBrowseHistory.class));
    }

    /**
     * 消息缺字段时抛出异常且不写库
     */
    @Test
    @DisplayName("onMessage：缺少 userId 时抛异常且不写库")
    void onMessageShouldRejectMissingField() {
        assertThatThrownBy(() -> consumer.onMessage("{\"roomId\":5}"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(rentalBrowseHistoryMapper);
    }

    /**
     * 消息体不是 JSON 时抛出异常且不写库
     */
    @Test
    @DisplayName("onMessage：非法 JSON 时抛异常且不写库")
    void onMessageShouldRejectInvalidJson() {
        assertThatThrownBy(() -> consumer.onMessage("not-a-json"))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(rentalBrowseHistoryMapper);
    }
}
