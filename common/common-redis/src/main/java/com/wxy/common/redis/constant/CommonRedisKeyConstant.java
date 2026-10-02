package com.wxy.common.redis.constant;

/**
 * Redis key 前缀常量：全局前缀 + common 自己的模块前缀。
 *
 * <p>key 统一为 {@code zza:{模块}:{业务}:{标识}}。{@link #PREFIX} 是全局起点，common 与服务都以它开头：
 * common 用 {@link #COMMON}；服务在自己的常量类里拼自己的模块前缀，例如
 * {@code InfraRedisKeyConstant.PREFIX = CommonRedisKeyConstant.PREFIX + "infra:"}。
 *
 * <p>模块前缀与具体业务键各写各的：服务不去引 common 的业务常量，common 也不去引服务的。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class CommonRedisKeyConstant {

    /** 全局前缀：所有模块的 key 都以它开头，服务在自己的常量类里引用它拼模块前缀 */
    public static final String PREFIX = "zza:";

    /** common 自己的模块前缀，common 的 key 都从这一段开始拼 */
    public static final String COMMON = PREFIX + "common:";

    /**
     * 工具类常量类，禁止实例化
     */
    private CommonRedisKeyConstant() {
    }
}
