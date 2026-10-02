package com.wxy.common.mq.constant;

/**
 * MQ topic / tag 常量：全局前缀 + common 自己的模块段。
 *
 * <p>topic 与 tag 只能用字母、数字、下划线、短横线（RocketMQ 不允许冒号），统一用短横线分隔，
 * 形如 {@code zza-{模块}-{业务}}。{@link #PREFIX} 是全局起点，common 与服务都以它开头：
 * common 用 {@link #COMMON}；服务在自己的常量类里拼自己的模块段，例如
 * {@code InfraMqConstant.PREFIX = CommonMqConstant.PREFIX + "-infra"}。
 *
 * <p>各模块的 topic / tag 各写各的，也没必要提供通用的拼 topic 方法。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class CommonMqConstant {

    /** 全局前缀：所有 topic 都以它开头，服务在自己的常量类里引用它拼模块段 */
    public static final String PREFIX = "zza";

    /** common 自己的模块段，common 的 topic 都从这一段开始拼 */
    public static final String COMMON = PREFIX + "-common";

    /**
     * 工具类常量类，禁止实例化
     */
    private CommonMqConstant() {
    }
}
