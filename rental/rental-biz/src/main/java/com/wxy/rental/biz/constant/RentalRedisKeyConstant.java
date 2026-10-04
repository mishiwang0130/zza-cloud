package com.wxy.rental.biz.constant;

import com.wxy.common.redis.constant.CommonRedisKeyConstant;

/**
 * rental 的 Redis key 前缀常量：全局前缀 + 本服务模块前缀。
 *
 * <p>key 统一为 {@code zza:rental:{业务}:{标识}}，具体拼接由 {@code RentalRedisKeyUtil} 提供，
 * 业务代码只引用常量与 Util，禁止硬编码字符串。
 *
 * <p>平台凭证缓存（访问凭证、续期凭证）不在这里：它是跨服务共享的缓存，
 * key 与缓存值定义在 common-redis（{@code CommonRedisKeyConstant.TOKEN} / {@code TokenCacheBO}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalRedisKeyConstant {

    /** rental 模块前缀：全局前缀 + 服务名 */
    public static final String PREFIX = CommonRedisKeyConstant.PREFIX + "rental:";

    /** 字典缓存：{@code zza:rental:dict:{字典类型编码}}，值为该类型下启用的「标签 + 值」列表 */
    public static final String DICT = PREFIX + "dict:";

    /**
     * 行政区划缓存：{@code zza:rental:area:tree}，值为扁平化的省市区列表。
     *
     * <p>不带业务标识是因为它只缓存一份：区划是标准数据，所有请求看的都是同一棵树。
     */
    public static final String AREA_TREE = PREFIX + "area:tree";

    /**
     * 工具类常量类，禁止实例化
     */
    private RentalRedisKeyConstant() {
    }
}
