package com.wxy.infra.biz.util;

import com.wxy.common.security.constant.TokenConstant;
import com.wxy.infra.biz.constant.InfraConstant;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.util.StringUtils;

/**
 * 凭证工具：生成续期凭证、去掉 {@code Bearer} 前缀。
 *
 * <p>凭证摘要不在本类里：库表与 Redis 都用 {@code DigestUtil.sha256Hex}，
 * 摘要算法必须与跨服务校验时一致，所以统一由 common-core 提供。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class InfraTokenUtil {

    /** 随机数发生器：{@link SecureRandom} 是线程安全的，全类共用一个实例即可 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 工具类，禁止实例化
     */
    private InfraTokenUtil() {
    }

    /**
     * 生成续期凭证：32 字节安全随机数，Base64 URL 编码（无填充），可直接放进 JSON 与 URL
     *
     * @return 续期凭证原始串
     */
    public static String generateRefreshToken() {
        byte[] bytes = new byte[InfraConstant.REFRESH_TOKEN_RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 去掉 {@code Authorization} 头里的 {@code Bearer} 前缀
     *
     * @param authorization 请求头原值，可以为 null
     * @return 裸 token，入参为空时返回 null
     */
    public static String stripBearer(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith(TokenConstant.PREFIX)) {
            return authorization.substring(TokenConstant.PREFIX.length()).trim();
        }
        // 兼容调用方直接传裸 token（例如内部联调）
        return authorization.trim();
    }
}
