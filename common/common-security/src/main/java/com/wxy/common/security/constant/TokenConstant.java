package com.wxy.common.security.constant;

/**
 * 凭证相关常量：token 前缀与 JWT 载荷字段名。
 *
 * <p>载荷字段名一旦确定就不要随意改动：已签发的 token 由网关与服务共同解析，
 * 改名会导致旧 token 全部解析失败，表现为用户集体掉线。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class TokenConstant {

    /** 凭证前缀：请求头格式为 "Bearer {token}" */
    public static final String PREFIX = "Bearer ";

    /** JWT 载荷字段：登录端类型，取值见 UserTypeEnum */
    public static final String CLAIM_USER_TYPE = "userType";

    /** JWT 载荷字段：用户名，仅用于日志与审计展示 */
    public static final String CLAIM_USERNAME = "username";

    /**
     * 工具类常量类，禁止实例化
     */
    private TokenConstant() {
    }
}
