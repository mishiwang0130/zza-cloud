package com.wxy.common.redis.constant;

/**
 * Redis key 前缀常量：全局前缀 + common 自己的模块前缀。
 *
 * <p>key 统一为 {@code zza:{模块}:{业务}:{标识}}。{@link #PREFIX} 是全局起点，common 与服务都以它开头：
 * common 用 {@link #COMMON}；服务在自己的常量类里拼自己的模块前缀，例如
 * {@code InfraRedisKeyConstant.PREFIX = CommonRedisKeyConstant.PREFIX + "infra:"}。
 *
 * <p>{@link #TOKEN} 与 {@link #REFRESH_TOKEN} 是例外：平台凭证缓存不属于某一个服务，
 * 而是「签发凭证的服务写、其他服务读」的共享缓存，所以直接用全局前缀，不带模块段。
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
     * 平台访问凭证缓存前缀：key 形如 {@code zza:token:{令牌摘要}}，值为 {@code TokenCacheBO}。
     *
     * <p>由签发凭证的服务（infra）在校验通过后写入，其他服务校验令牌时可以直接读同一份缓存，
     * 未命中再回源调签发方——这样既保证「凭证状态以签发方为准」，又省掉大部分跨服务调用。
     */
    public static final String TOKEN = PREFIX + "token:";

    /**
     * 平台续期凭证缓存前缀：key 形如 {@code zza:refresh:{凭证摘要}}，值与访问凭证缓存同结构。
     */
    public static final String REFRESH_TOKEN = PREFIX + "refresh:";

    /**
     * 工具类常量类，禁止实例化
     */
    private CommonRedisKeyConstant() {
    }
}
