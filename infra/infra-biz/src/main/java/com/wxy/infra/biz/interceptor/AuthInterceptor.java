package com.wxy.infra.biz.interceptor;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.infra.biz.service.InfraTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录凭证拦截器：校验 {@code Authorization} 头里的 token，并把登录用户写入上下文。
 *
 * <p>每一端注册一个实例，期望的登录端类型在建实例时就绑定好（admin 端要求 {@code user_type = 1}，
 * app 端要求 {@code 2}），请求进来只做校验、不做端类型推断。
 *
 * <p>刻意不做「不是 admin 前缀就算 app 端」这类兜底推断：端前缀与端类型是一一对应的，
 * 用兜底推断意味着以后新增端（内部接口、开放平台等）会被静默当成 app 端校验，属于安全默认值错误。
 * 新增端时在 {@code WebConfig} 里按现有写法再注册一组拦截器即可。
 *
 * <p>校验失败抛 {@code UnauthorizedException}，由 common 的全局异常处理器统一返回 HTTP 401；
 * 本拦截器不吞异常、不放行匿名请求，保证服务在网关之后仍是独立可信的一道校验。
 *
 * @author wxy
 * @date 2026/10/03
 */
public class AuthInterceptor implements HandlerInterceptor {

    /** 凭证服务 */
    private final InfraTokenService tokenService;

    /** 本实例负责的登录端类型：注册时绑定，不在请求期推断 */
    private final UserTypeEnum userType;

    /**
     * 构造拦截器
     *
     * @param tokenService 凭证服务
     * @param userType     本实例负责的登录端类型
     */
    public AuthInterceptor(InfraTokenService tokenService, UserTypeEnum userType) {
        this.tokenService = tokenService;
        this.userType = userType;
    }

    /**
     * 校验凭证并写入登录上下文
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  处理器
     * @return 恒为 true；校验不过直接抛异常中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader(HeaderConstant.AUTHORIZATION);
        LoginUser loginUser = tokenService.validate(authorization, userType);
        UserContextHolder.set(loginUser);
        return true;
    }
}
