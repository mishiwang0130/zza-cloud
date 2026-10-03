package com.wxy.common.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 摘要工具：目前只提供 SHA-256 十六进制摘要。
 *
 * <p>凭证相关的地方都用它：库表里的 token 摘要、Redis 缓存 key 的摘要、跨服务校验时重新计算的摘要，
 * 必须由同一份实现产出，否则「同一个令牌算出不同的摘要」会出现查不到、校验不过这类难查的问题。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class DigestUtil {

    /**
     * 工具类，禁止实例化
     */
    private DigestUtil() {
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
}
