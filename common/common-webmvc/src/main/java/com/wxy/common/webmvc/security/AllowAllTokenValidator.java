package com.wxy.common.webmvc.security;

import com.wxy.common.core.constant.CommonConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.security.TokenValidator;

/**
 * 放行实现：不做任何凭证校验，任何请求都当成系统身份。
 *
 * <p>只给「确实不需要鉴权」的服务用——这类服务必须显式注册它（或自己写一个等价实现），
 * 让「本服务不做登录校验」这个决定留在代码里、出现在代码评审中；
 * 公共拦截器不会因为缺少实现就默认放行，缺实现会直接启动失败。
 *
 * <p>返回的登录用户端类型为 null，表示不区分端：公共拦截器会跳过端类型比对，
 * 否则没有端概念的服务会连自己的接口都调不通。
 *
 * <p><b>不要给对外提供业务接口的服务用</b>：注册它就等于所有接口匿名可访问。
 *
 * @author wxy
 * @date 2026/10/03
 */
public class AllowAllTokenValidator implements TokenValidator {

    /**
     * 放行时使用的固定身份：系统用户，不区分登录端。
     */
    private static final LoginUser SYSTEM_USER =
            new LoginUser(CommonConstant.SYSTEM_USER_ID, null, "anonymous");

    /**
     * 不校验令牌，直接返回系统身份
     *
     * @param token 裸令牌（已去掉 Bearer 前缀），可以为 null
     * @return 系统身份
     */
    @Override
    public LoginUser validate(String token) {
        return SYSTEM_USER;
    }
}
