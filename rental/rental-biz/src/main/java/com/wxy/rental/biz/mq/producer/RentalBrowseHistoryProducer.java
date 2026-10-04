package com.wxy.rental.biz.mq.producer;

import com.alibaba.fastjson2.JSON;
import com.wxy.rental.biz.constant.RentalMqConstant;
import com.wxy.rental.biz.mq.message.RentalBrowseHistoryMsg;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 浏览记录消息生产者：App 房间详情接口在返回详情的同时调它补写一条浏览流水。
 *
 * <p><b>发送失败绝不影响详情返回</b>：浏览记录是「顺带」产生的数据，详情该展示还能展示，因此这里把所有异常都吞成 warn 日志，调用方不需要（也不该）为它写补偿逻辑。
 *
 * <p>消息体统一用 Fastjson2 转成 JSON 字符串发送，不用 RocketMQ 默认的消息转换器：这样消息在控制台里可以直接读，格式也只由 rental 自己决定，不会因为换个转换器就变形状。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Component
public class RentalBrowseHistoryProducer {

    /**
     * RocketMQ 模板：用 {@link ObjectProvider} 而不是直接注入。
     *
     * <p>没开 MQ 的环境（例如只想跑管理端接口）也应当能启动，缺模板时这里记一条 warn 跳过，而不是让整个服务起不来。
     *
     * <p>模板不存在有两种原因，都要查配置：{@code rocketmq.name-server} 没配；或它与 {@code rocketmq.producer.group}
     * 只配了一个——starter 建 DefaultMQProducer 要求两个属性同时存在，少一个模板 Bean 就不会创建。
     */
    @Resource
    private ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    /**
     * 发送一条浏览记录消息
     *
     * @param userId 浏览用户 ID；为 null（未登录浏览）时不发送，也不影响调用方
     * @param roomId 被浏览的房间 ID
     */
    public void send(Long userId, Long roomId) {
        if (userId == null || roomId == null) {
            return;
        }
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            log.warn("[send][RocketMQTemplate 不存在，检查 rocketmq.name-server 与 rocketmq.producer.group，跳过浏览记录]"
                    + " userId={}, roomId={}", userId, roomId);
            return;
        }
        String destination = RentalMqConstant.BROWSE_HISTORY_TOPIC + ":" + RentalMqConstant.BROWSE_HISTORY_TAG;
        String payload = JSON.toJSONString(new RentalBrowseHistoryMsg(userId, roomId, LocalDateTime.now()));
        try {
            rocketMQTemplate.syncSend(destination, payload);
        } catch (RuntimeException ex) {
            log.warn("[send][发送浏览记录消息失败，不影响详情返回] userId={}, roomId={}, error={}",
                    userId, roomId, ex.getMessage(), ex);
        }
    }
}
