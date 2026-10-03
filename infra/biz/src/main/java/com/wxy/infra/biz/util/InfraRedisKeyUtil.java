package com.wxy.infra.biz.util;

import com.wxy.infra.biz.constant.InfraRedisKeyConstant;

/**
 * infra 的 Redis key 拼接工具：一个 key 一个方法，key 长什么样只在这里定义。
 *
 * <p>刻意不提供通用的 {@code buildKey(...)}：通用拼接会把 key 结构推给调用方，
 * 一旦结构调整就要满仓库找调用点。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class InfraRedisKeyUtil {

    /**
     * 工具类，禁止实例化
     */
    private InfraRedisKeyUtil() {
    }

    /**
     * access token 校验缓存 key
     *
     * @param tokenHash token 的 SHA-256 摘要
     * @return 完整 key
     */
    public static String accessTokenKey(String tokenHash) {
        return InfraRedisKeyConstant.TOKEN + tokenHash;
    }

    /**
     * refresh token 校验缓存 key
     *
     * @param tokenHash 续期凭证的 SHA-256 摘要
     * @return 完整 key
     */
    public static String refreshTokenKey(String tokenHash) {
        return InfraRedisKeyConstant.REFRESH_TOKEN + tokenHash;
    }

    /**
     * 用户权限集合缓存 key
     *
     * @param userId 用户 ID
     * @return 完整 key
     */
    public static String userPermissionKey(Long userId) {
        return InfraRedisKeyConstant.USER_PERMISSION + userId;
    }
}
