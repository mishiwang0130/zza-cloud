package com.wxy.infra.biz.util;

import com.wxy.common.security.constant.TokenConstant;
import com.wxy.infra.biz.constant.InfraConstant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.util.StringUtils;

/**
 * 凭证工具：生成续期凭证、计算凭证摘要、去掉 {@code Bearer} 前缀。
 *
 * <p>摘要统一用 SHA-256（十六进制小写）作为库表与 Redis 的查找键：
 * 库里不落原始 token，日志与缓存里也不会出现可直接冒用的凭证。
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
     * 计算字符串的 SHA-256 摘要
     *
     * @param raw 原始字符串，可以为 null
     * @return 十六进制小写摘要，入参为 null 时返回 null
     */
    public static String sha256Hex(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 是 JDK 必须支持的算法，走到这里说明运行环境异常，属于不可恢复错误
            throw new IllegalStateException("当前运行环境不支持 SHA-256", ex);
        }
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
