package com.wxy.infra.biz.service;

import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.app.AuthAppLoginReqVO;
import com.wxy.infra.biz.vo.app.AuthAppRefreshReqVO;
import com.wxy.infra.biz.vo.app.AuthAppUpdateProfileReqVO;
import com.wxy.infra.biz.vo.app.AppAuthUserInfoRespVO;

/**
 * @author wxy
 * @description 用户端登录校验
 * @date 2026/10/04
 */

public interface InfraAppAuthService {

    /**
     * 管理用户登录
     *
     * @param reqVO   登录入参
     * @param loginIp 登录 IP，可为空
     * @return 凭证返回体
     */
    AuthTokenRespVO login(AuthAppLoginReqVO reqVO, String loginIp);

    /**
     * 用户端续期
     *
     * <p>只接受 {@code user_type=2}（用户端）的续期凭证：管理后台的续期凭证不能用来换用户端凭证，
     * 反之亦然，避免两端凭证互相越权。轮换后旧续期凭证立即失效。
     *
     * @param reqVO 续期入参
     * @return 新的凭证返回体
     */
    AuthTokenRespVO refresh(AuthAppRefreshReqVO reqVO);

    /**
     * 用户端登出：当前访问凭证与同一登录会话的续期凭证一并失效
     *
     * @param authorization 当前请求的凭证头，可以为空
     */
    void logout(String authorization);

    /**
     * 查询当前登录用户信息（个人中心）
     *
     * @return 用户信息，手机号脱敏
     */
    AppAuthUserInfoRespVO getUserInfo();

    /**
     * 修改当前登录用户的个人资料（昵称、头像）
     *
     * @param reqVO 修改入参
     */
    void updateProfile(AuthAppUpdateProfileReqVO reqVO);
}
