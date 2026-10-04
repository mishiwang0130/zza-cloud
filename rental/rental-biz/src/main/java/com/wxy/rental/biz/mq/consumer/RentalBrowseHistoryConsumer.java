package com.wxy.rental.biz.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.wxy.rental.biz.constant.RentalMqConstant;
import com.wxy.rental.biz.mapper.RentalBrowseHistoryMapper;
import com.wxy.rental.biz.mq.message.RentalBrowseHistoryMsg;
import com.wxy.rental.biz.po.RentalBrowseHistory;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 浏览记录消息消费者：收到一条消息就往 {@code rental_browse_history} 插一条流水。
 *
 * <p><b>纯插入、不去重</b>：同一用户浏览同一房间多次就是多条流水，这正是浏览记录的意义。
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

    /** 浏览记录 Mapper */
    @Resource
    private RentalBrowseHistoryMapper rentalBrowseHistoryMapper;

    /**
     * 消费浏览记录消息
     *
     * <p>单条插入本身是原子的，加事务是为了与仓库「写库方法显式声明回滚范围」的规范保持一致。
     *
     * @param message 消息体（Fastjson2 序列化的 JSON 字符串）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(String message) {
        RentalBrowseHistoryMsg msg = parseMessage(message);
        RentalBrowseHistory po = new RentalBrowseHistory();
        po.setUserId(msg.getUserId());
        po.setRoomId(msg.getRoomId());
        // 浏览时间就是 create_time：用消息里的时间而不是消费时刻，消费延迟时记录的时间仍然准确；
        // 审计填充器只在字段为 null 时补值，显式赋值会被尊重
        po.setCreateTime(msg.getBrowseTime() == null ? LocalDateTime.now() : msg.getBrowseTime());
        rentalBrowseHistoryMapper.insert(po);
        log.debug("[onMessage][浏览记录已落库] userId={}, roomId={}", po.getUserId(), po.getRoomId());
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
