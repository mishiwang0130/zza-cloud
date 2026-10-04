package com.wxy.infra.biz.service;

import com.wxy.infra.biz.vo.admin.AuthLoginReqVO;
import com.wxy.infra.biz.vo.admin.AuthMenuRespVO;
import com.wxy.infra.biz.vo.admin.AuthRefreshReqVO;
import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.admin.AuthUpdatePasswordReqVO;
import com.wxy.infra.biz.vo.admin.AuthUserInfoRespVO;
import java.util.List;

/**
 * 管理后台认证服务：登录、登出、续期、当前用户信息与自身密码维护。
 *
 * <p>登录只实现 admin 端；app 端登录由后续需求补，届时复用 {@link InfraTokenService} 签发凭证即可。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface InfraAdminAuthService {

    /**
     * 管理后台登录
     *
     * @param reqVO   登录入参
     * @param loginIp 登录 IP，可为空
     * @return 凭证返回体
     */
    AuthTokenRespVO login(AuthLoginReqVO reqVO, String loginIp);

    /**
     * 续期
     *
     * @param reqVO 续期入参
     * @return 新的凭证返回体
     */
    AuthTokenRespVO refresh(AuthRefreshReqVO reqVO);

    /**
     * 登出
     *
     * @param authorization 当前请求的凭证头
     */
    void logout(String authorization);

    /**
     * 查询当前登录用户信息（含角色与权限）
     *
     * @return 用户信息
     */
    AuthUserInfoRespVO getUserInfo();

    /**
     * 查询当前登录用户的导航菜单树
     *
     * @return 菜单树
     */
    List<AuthMenuRespVO> listMenus();

    /**
     * 修改当前登录用户的密码
     *
     * @param reqVO 修改密码入参
     */
    void updatePassword(AuthUpdatePasswordReqVO reqVO);
}
