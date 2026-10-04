/**
 * rental 的消息消费者：目前预留给房源浏览记录。
 *
 * <p>按契约稿，消费者（{@code RentalBrowseHistoryConsumer}，挂 {@code @RocketMQMessageListener}）
 * 收到消息后只做一件事：往 {@code rental_browse_history} 纯插入一条流水，不去重
 * （同一房间浏览多次就是多条）；消费异常抛出让 RocketMQ 重试，绝不吞掉。
 *
 * <p>消息体 {@code RentalBrowseHistoryMsg} 与 topic / tag 已在 {@code RentalMqConstant} 就位，
 * 消费者本身与 App 端接口一起在后续窗口实现。
 *
 * @author wxy
 * @date 2026/10/04
 */
package com.wxy.rental.biz.mq.consumer;
