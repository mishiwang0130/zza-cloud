package com.wxy.infra.biz.interceptor;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.webmvc.config.WebProperties;
import com.wxy.infra.biz.service.InfraTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录凭证拦截器：校验 {@code Authorization} 头里的 token，并把登录用户写入上下文。
 *
 * <p>两端通用：admin 端请求要求 token 的 {@code user_type = 1}，app 端要求 {@code 2}，
 * 期望端由请求路径前缀判定（与端前缀自动拼接用的是同一份 {@code WebProperties} 配置）。
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

    /** Web 层配置：提供端前缀，用于判定请求属于哪一端 */
    private final WebProperties webProperties;

    /**
     * 构造拦截器
     *
     * @param tokenService  凭证服务
     * @param webProperties Web 层配置
     */
    public AuthInterceptor(InfraTokenService tokenService, WebProperties webProperties) {
        this.tokenService = tokenService;
        this.webProperties = webProperties;
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
        LoginUser loginUser = tokenService.validate(authorization, resolveUserType(request));
        UserContextHolder.set(loginUser);
        return true;
    }

    /**
     * 判定当前请求属于哪一端
     *
     * <p>本拦截器只注册在两端前缀下，因此「不是 admin 前缀」即按 app 端处理；
     * 判定依据是配置里的前缀，改前缀配置时不用改代码。
     *
     * @param request 当前请求
     * @return 期望的登录端类型
     */
    private UserTypeEnum resolveUserType(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith(webProperties.getAdminApiPrefix() + "/")) {
            return UserTypeEnum.ADMIN;
        }
        return UserTypeEnum.APP;
    }
}
