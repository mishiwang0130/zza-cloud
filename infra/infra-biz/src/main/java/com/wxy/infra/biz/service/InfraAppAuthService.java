package com.wxy.infra.biz.service;

import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.app.AuthAppLoginReqVO;

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

}
