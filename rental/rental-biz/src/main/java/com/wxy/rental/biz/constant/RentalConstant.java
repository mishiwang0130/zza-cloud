package com.wxy.rental.biz.constant;

/**
 * rental 的跨模块常量：目前只有缓存有效期。
 *
 * <p>缓存有效期单独放这里而不是写在业务类里，是为了让「哪些缓存活多久」一眼可见：
 * 过期时间长短是运维可感知的行为，散落在各个 Service 里很容易被改乱。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalConstant {

    /** 字典缓存有效期（秒）：字典改动少但会改，30 分钟足够短到能接受、长到能省掉绝大多数远程调用 */
    public static final long DICT_CACHE_SECONDS = 1800L;

    /** 行政区划缓存有效期（秒）：区划是标准数据几乎不变，缓存一天，避免每次都拉全量 */
    public static final long AREA_TREE_CACHE_SECONDS = 86400L;

    /** 行政区划层级：省级，与 infra 的 {@code InfraAreaLevelEnum} 口径一致 */
    public static final Integer AREA_LEVEL_PROVINCE = 1;

    /** 行政区划层级：市级 */
    public static final Integer AREA_LEVEL_CITY = 2;

    /** 行政区划层级：区县级；公寓存的是区县 ID，按市筛选时展开出来的也是这一层 */
    public static final Integer AREA_LEVEL_DISTRICT = 3;

    /**
     * 工具类常量类，禁止实例化
     */
    private RentalConstant() {
    }
}
