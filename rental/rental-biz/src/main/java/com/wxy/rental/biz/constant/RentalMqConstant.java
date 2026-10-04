package com.wxy.rental.biz.constant;

import com.wxy.common.mq.constant.CommonMqConstant;

/**
 * rental 的 MQ topic / tag 常量：全局前缀 + 本服务模块段 + 具体业务。
 *
 * <p>topic / tag 只能用字母、数字、下划线、短横线（RocketMQ 不允许冒号），
 * 生产者和消费者引用同一份常量，避免两边字符串写岔导致消息进了没人听的 topic。
 *
 * <p>目前只有浏览记录一条链路：App 房间详情接口在返回详情的同时发一条浏览消息，
 * 消费者纯插入一条流水（不去重）。生产者和消费者本期不写（等 infra 的 App 用户与登录），
 * 这里先把 topic / tag 定下来，App 接口与消费者直接引用即可。
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
     * 工具类常量类，禁止实例化
     */
    private RentalMqConstant() {
    }
}
