package com.wxy.rental.biz.mq.message;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 房源浏览记录消息体：App 房间详情接口发出的浏览流水。
 *
 * <p>字段刻意只有三个：消费者要做的只是「往 {@code rental_browse_history} 插一条流水」，
 * 多带字段就会让消费者有能力做别的事（比如顺带更新房间），链路一长就没人说得清
 * 一次浏览到底改了什么。
 *
 * <p>消息体用 Fastjson2 转成 JSON 字符串收发（见 {@code RentalMqConstant}），
 * 不用 RocketMQ 默认的 Jackson 转换器，与「业务代码 JSON 统一用 Fastjson2」的规范一致。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalBrowseHistoryMsg implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 浏览用户 ID：从登录上下文取，未登录时不发消息 */
    private Long userId;

    /** 被浏览的房间 ID */
    private Long roomId;

    /** 浏览时间：由生产端取当前时间传入，消费端按它落 create_time */
    private LocalDateTime browseTime;
}
