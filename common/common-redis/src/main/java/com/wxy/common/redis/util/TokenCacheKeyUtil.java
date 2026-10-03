package com.wxy.common.redis.util;

import com.wxy.common.redis.constant.CommonRedisKeyConstant;

/**
 * 平台凭证缓存的 key 拼接工具：一个 key 一个方法，key 长什么样只在这里定义。
 *
 * <p>入参统一是令牌**摘要**而不是原始令牌：签发方手里本来就有摘要（要落库），
 * 这里避免「传原始令牌被再摘要一次」这类静默错误；手里只有原始令牌时，
 * 先用 {@code DigestUtil.sha256Hex} 算摘要，摘要算法必须全平台一致。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class TokenCacheKeyUtil {

    /**
     * 工具类，禁止实例化
     */
    private TokenCacheKeyUtil() {
    }

    /**
     * 访问凭证的缓存 key
     *
     * @param tokenHash 访问令牌的 SHA-256 摘要
     * @return 完整 key，形如 {@code zza:token:{摘要}}
     */
    public static String accessTokenKey(String tokenHash) {
        return CommonRedisKeyConstant.TOKEN + tokenHash;
    }

    /**
     * 续期凭证的缓存 key
     *
     * @param tokenHash 续期凭证的 SHA-256 摘要
     * @return 完整 key，形如 {@code zza:refresh:{摘要}}
     */
    public static String refreshTokenKey(String tokenHash) {
        return CommonRedisKeyConstant.REFRESH_TOKEN + tokenHash;
    }
}
