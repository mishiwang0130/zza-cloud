package com.wxy.rental.biz.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.wxy.rental.biz.constant.RentalMqConstant;
import com.wxy.rental.biz.mq.message.RentalBrowseHistoryMsg;
import com.wxy.rental.biz.service.RentalBrowseHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 浏览记录消息消费者：把消息交给 {@link RentalBrowseHistoryService} 落库。
 *
 * <p>去重与时间口径由服务负责（同一用户看同一房间只留一条、刷新浏览时间），这里只做「消息 → 参数」的适配与格式校验。
 *
 * <p><b>异常一律抛出</b>：解析不出消息体、字段缺失、写库失败都直接抛，让 RocketMQ 走失败重试、超过重试次数进死信队列；这里刻意不 catch 后吞掉，吞掉等于静默丢数据。
 *
 * <p>按契约稿 §6，浏览记录模块没有业务错误码，所以这里抛的是普通运行时异常，不套 {@code BizException}（那是给接口返回体用的，MQ 消费链路没有接口响应）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = RentalMqConstant.BROWSE_HISTORY_TOPIC,
        selectorExpression = RentalMqConstant.BROWSE_HISTORY_TAG,
        consumerGroup = RentalMqConstant.BROWSE_HISTORY_CONSUMER_GROUP)
public class RentalBrowseHistoryConsumer implements RocketMQListener<String> {

    /** 浏览记录写入服务 */
    @Resource
    private RentalBrowseHistoryService rentalBrowseHistoryService;

    /**
     * 消费浏览记录消息
     *
     * @param message 消息体（Fastjson2 序列化的 JSON 字符串）
     */
    @Override
    public void onMessage(String message) {
        RentalBrowseHistoryMsg msg = parseMessage(message);
        rentalBrowseHistoryService.record(msg.getUserId(), msg.getRoomId(), msg.getBrowseTime());
        log.debug("[onMessage][浏览记录已落库] userId={}, roomId={}", msg.getUserId(), msg.getRoomId());
    }

    /**
     * 解析并校验消息体
     *
     * @param message 原始消息
     * @return 消息体
     * @throws IllegalArgumentException 消息缺少必填字段
     */
    private RentalBrowseHistoryMsg parseMessage(String message) {
        RentalBrowseHistoryMsg msg;
        try {
            msg = JSON.parseObject(message, RentalBrowseHistoryMsg.class);
        } catch (RuntimeException ex) {
            log.error("[parseMessage][浏览记录消息解析失败] message={}", message, ex);
            throw ex;
        }
        if (msg == null || msg.getUserId() == null || msg.getRoomId() == null) {
            // 字段缺失的消息重试多少次都救不回来，仍然抛出交给 RocketMQ 的重试与死信队列，避免静默丢数据
            log.error("[parseMessage][浏览记录消息缺少必填字段] message={}", message);
            throw new IllegalArgumentException("浏览记录消息缺少 userId 或 roomId：" + message);
        }
        return msg;
    }
}
