package com.wxy.rental.biz.util;

import com.wxy.rental.biz.constant.RentalRedisKeyConstant;

/**
 * rental 的 Redis key 拼接工具：一个 key 一个方法，key 长什么样只在这里定义。
 *
 * <p>刻意不提供通用的 {@code buildKey(...)}：通用拼接会把 key 结构推给调用方，
 * 一旦结构调整就要满仓库找调用点。
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalRedisKeyUtil {

    /**
     * 工具类，禁止实例化
     */
    private RentalRedisKeyUtil() {
    }

    /**
     * 字典缓存 key
     *
     * @param dictType 字典类型编码
     * @return 完整 key
     */
    public static String dictKey(String dictType) {
        return RentalRedisKeyConstant.DICT + dictType;
    }

    /**
     * 行政区划缓存 key
     *
     * @return 完整 key
     */
    public static String areaTreeKey() {
        return RentalRedisKeyConstant.AREA_TREE;
    }
}
