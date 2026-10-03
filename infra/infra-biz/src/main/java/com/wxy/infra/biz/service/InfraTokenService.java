package com.wxy.infra.biz.service;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.infra.biz.vo.admin.AuthTokenRespVO;

/**
 * 凭证服务（admin 端与 app 端共用）：签发、校验、续期与失效。
 *
 * <p>所有方法都与端无关，由调用方传入 {@code userType} 决定凭证属于哪一端；
 * 校验时端类型必须与请求所在端一致，避免 app 端凭证越权访问管理后台接口。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface InfraTokenService {

    /**
     * 签发一对凭证并落库
     *
     * @param userId   用户 ID
     * @param userType 登录端类型
     * @param username 登录用户名
     * @param loginIp  登录 IP，可为空
     * @return 凭证返回体（access token + 续期凭证）
     */
    AuthTokenRespVO createTokenPair(Long userId, UserTypeEnum userType, String username, String loginIp);

    /**
     * 校验访问凭证
     *
     * <p>校验顺序：验签与过期 → 查 Redis 缓存 → 未命中查 MySQL 并回写缓存 → 端类型匹配；
     * 任何一步不通过都抛 {@code UnauthorizedException}（HTTP 401）。
     *
     * @param authorization   请求头原值，支持带 {@code Bearer } 前缀或裸 token
     * @param expectUserType  期望的登录端类型；为 null 表示该接口不属于任何端，只校验凭证有效性、不比对端类型
     * @return 登录用户
     */
    LoginUser validate(String authorization, UserTypeEnum expectUserType);

    /**
     * 用续期凭证换取新的一对凭证（旧续期凭证立即失效）
     *
     * @param refreshToken   续期凭证原值
     * @param expectUserType 期望的登录端类型
     * @return 新的凭证返回体
     */
    AuthTokenRespVO refresh(String refreshToken, UserTypeEnum expectUserType);

    /**
     * 失效当前凭证：access token 立即失效，同一登录会话的续期凭证一并失效
     *
     * <p>幂等：凭证不存在或已失效时不报错。
     *
     * @param authorization 请求头原值
     */
    void revoke(String authorization);

    /**
     * 失效某个用户的全部有效凭证
     *
     * <p>用于改密后强制重新登录、管理员强制下线等场景：一次把该用户所有会话的
     * access token 与续期凭证都置为失效。
     *
     * @param userId 用户 ID，为 null 时忽略
     */
    void revokeAll(Long userId);
}
