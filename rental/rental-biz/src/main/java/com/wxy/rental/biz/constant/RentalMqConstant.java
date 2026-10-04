package com.wxy.rental.biz.constant;

import com.wxy.common.mq.constant.CommonMqConstant;

/**
 * rental 的 MQ topic / tag 常量：全局前缀 + 本服务模块段 + 具体业务。
 *
 * <p>topic / tag 只能用字母、数字、下划线、短横线（RocketMQ 不允许冒号），
 * 生产者和消费者引用同一份常量，避免两边字符串写岔导致消息进了没人听的 topic。
 *
 * <p>目前只有浏览记录一条链路：App 房间详情接口在返回详情的同时发一条浏览消息，
 * 消费者纯插入一条流水（不去重）。topic / tag / 消费者组都只在这里定义，
 * 生产者、消费者与后续可能的补偿任务引用同一份常量。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalMqConstant {

    /** rental 模块段：全局前缀 + 服务名，topic 都从这一段开始拼 */
    public static final String PREFIX = CommonMqConstant.PREFIX + "-rental";

    /** 浏览记录 topic：App 房间详情发、浏览记录消费者收 */
    public static final String BROWSE_HISTORY_TOPIC = PREFIX + "-room-browse-history";

    /** 浏览记录 tag：写入一条浏览流水 */
    public static final String BROWSE_HISTORY_TAG = "room-browse";

    /**
     * 浏览记录消费者组：同一组内的实例分摊消息。
     *
     * <p>服务多实例部署时，同一个 consumerGroup 保证一条消息只被一个实例消费一次，
     * 组名带服务前缀，避免与其他服务的消费者组重名。
     */
    public static final String BROWSE_HISTORY_CONSUMER_GROUP = PREFIX + "-room-browse-history-consumer";

    /**
     * 工具类常量类，禁止实例化
     */
    private RentalMqConstant() {
    }
}
