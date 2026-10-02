package com.wxy.common.security.util;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.security.constant.TokenConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.util.StringUtils;

/**
 * JWT 工具：签发与解析凭证。
 *
 * <p>密钥与有效期在构造时确定，配置非法（密钥缺失、长度不足 32 字节、有效期非正数）
 * 直接抛异常让应用启动失败：鉴权配置错误必须在启动阶段暴露，不能等到线上请求才报错。
 *
 * <p>解析失败统一抛 {@link UnauthorizedException}，由全局异常处理器映射成 HTTP 401。
 * 调用方不需要区分格式错误、签名不合法与已过期，对客户端来说都是重新登录。
 *
 * @author wxy
 * @date 2026/10/02
 */
public class JwtUtil {

    /** HS256 要求的最小密钥长度（字节） */
    private static final int MIN_SECRET_BYTES = 32;

    /** 签名密钥 */
    private final SecretKey secretKey;

    /** token 有效期（秒） */
    private final long expireSeconds;

    /**
     * 构造 JWT 工具并校验配置
     *
     * @param secret        签名密钥，至少 32 字节
     * @param expireSeconds 有效期（秒），必须大于 0
     */
    public JwtUtil(String secret, long expireSeconds) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT 密钥未配置或长度不足 " + MIN_SECRET_BYTES + " 字节，请通过环境变量注入");
        }
        if (expireSeconds <= 0) {
            throw new IllegalStateException("JWT 有效期必须大于 0，当前值：" + expireSeconds);
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    /**
     * 签发 token
     *
     * @param loginUser 登录用户
     * @return token 字符串，不含 "Bearer " 前缀
     */
    public String generate(LoginUser loginUser) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.userId()))
                .claim(TokenConstant.CLAIM_USER_TYPE, loginUser.userType())
                .claim(TokenConstant.CLAIM_USERNAME, loginUser.username())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token
     *
     * @param token token 字符串，可以带 "Bearer " 前缀
     * @return 登录用户
     * @throws UnauthorizedException token 为空、格式错误、签名不合法或已过期
     */
    public LoginUser parse(String token) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(removePrefix(token))
                    .getPayload();
            return new LoginUser(Long.valueOf(claims.getSubject()), readUserType(claims),
                    claims.get(TokenConstant.CLAIM_USERNAME, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("登录凭证无效或已过期");
        }
    }

    /**
     * 判断 token 是否有效
     *
     * @param token token 字符串
     * @return 有效返回 true
     */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 获取 token 有效期
     *
     * @return 有效期（秒）
     */
    public long getExpireSeconds() {
        return expireSeconds;
    }

    /**
     * 去掉 "Bearer " 前缀，兼容调用方直接传裸 token
     *
     * @param token token 字符串
     * @return 裸 token
     */
    private static String removePrefix(String token) {
        if (token.startsWith(TokenConstant.PREFIX)) {
            return token.substring(TokenConstant.PREFIX.length()).trim();
        }
        return token.trim();
    }

    /**
     * 读取端类型：JWT 反序列化后数字可能是 Integer 或 Long，统一按 Number 处理
     *
     * @param claims 载荷
     * @return 端类型，未携带时返回 null
     */
    private static Integer readUserType(Claims claims) {
        Object raw = claims.get(TokenConstant.CLAIM_USER_TYPE);
        return raw instanceof Number number ? number.intValue() : null;
    }
}
