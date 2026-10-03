package com.wxy.infra.biz.interceptor;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.webmvc.config.WebProperties;
import com.wxy.infra.biz.config.InfraSecurityProperties;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.util.InfraTokenUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录凭证拦截器：校验访问令牌，并把登录用户写入上下文。
 *
 * <p>一个拦截器覆盖两端，职责分三步：
 * <ol>
 *   <li>标注 {@link PermitAll} 的接口直接放行（登录、续期等免登录接口，不需要维护路径白名单）；</li>
 *   <li>取令牌：优先 {@code Authorization} 头，其次 {@code ?token=} 请求参数（WebSocket、SSE 场景）；</li>
 *   <li>校验令牌并把身份写入上下文，期望的端类型由接口前缀判定。</li>
 * </ol>
 *
 * <p>端类型判定只认约定前缀（{@code /admin-api} → 管理后台、{@code /app-api} → 用户端），
 * 判不出来时不做比对——不做「否则就当某一端」的兜底推断，否则新增端（WebSocket、开放接口等）
 * 会被静默按某一端校验。
 *
 * <p>不做「信任网关透传身份」的快速通道：本服务自己校验令牌，因此即使网关没配好、
 * 请求头被伪造，也不会出现凭请求头就拿到身份的情况。网关侧的伪造头清洗是另一道独立防线。
 *
 * <p>校验失败抛 {@code UnauthorizedException}，由 common 的全局异常处理器统一返回 HTTP 401。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    /** 凭证服务 */
    private final InfraTokenService tokenService;

    /** Web 层配置：提供端前缀，用于判定请求属于哪一端 */
    private final WebProperties webProperties;

    /** 安全配置：令牌参数名与模拟登录开关 */
    private final InfraSecurityProperties securityProperties;

    /**
     * 构造拦截器
     *
     * @param tokenService       凭证服务
     * @param webProperties      Web 层配置
     * @param securityProperties 安全配置
     */
    public AuthInterceptor(InfraTokenService tokenService, WebProperties webProperties,
                           InfraSecurityProperties securityProperties) {
        this.tokenService = tokenService;
        this.webProperties = webProperties;
        this.securityProperties = securityProperties;
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
        if (isPermitAll(handler)) {
            return true;
        }
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException();
        }
        UserContextHolder.set(resolveLoginUser(token, resolveExpectedUserType(request)));
        return true;
    }

    /**
     * 判断接口是否允许匿名访问
     *
     * @param handler 处理器
     * @return 方法或所在类标注了 {@link PermitAll} 时返回 true
     */
    private boolean isPermitAll(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(PermitAll.class)
                || handlerMethod.getBeanType().isAnnotationPresent(PermitAll.class);
    }

    /**
     * 从请求中取访问令牌
     *
     * <p>优先标准请求头，其次请求参数：参数方式是为了兼容 WebSocket、SSE 这类无法设置请求头的场景。
     *
     * @param request 当前请求
     * @return 裸令牌（已去掉 Bearer 前缀），没有携带时返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HeaderConstant.AUTHORIZATION);
        String token = InfraTokenUtil.stripBearer(authorization);
        if (StringUtils.hasText(token)) {
            return token;
        }
        String parameterName = securityProperties.getTokenParameter();
        if (!StringUtils.hasText(parameterName)) {
            return null;
        }
        String parameterToken = request.getParameter(parameterName);
        return StringUtils.hasText(parameterToken) ? parameterToken.trim() : null;
    }

    /**
     * 按接口前缀判定请求属于哪一端
     *
     * <p>判不出来返回 null，表示不比对端类型：此时令牌本身仍必须有效，只是不再校验它是哪一端签发的。
     * 这样没有端前缀的接口（例如将来的 WebSocket 连接地址）不会被误判成某一端。
     *
     * @param request 当前请求
     * @return 期望的登录端类型，前缀不在约定范围内时返回 null
     */
    private UserTypeEnum resolveExpectedUserType(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith(webProperties.getAdminApiPrefix())) {
            return UserTypeEnum.ADMIN;
        }
        if (uri.startsWith(webProperties.getAppApiPrefix())) {
            return UserTypeEnum.APP;
        }
        return null;
    }

    /**
     * 校验令牌得到登录用户
     *
     * <p>真实校验失败后才会尝试模拟登录：模拟登录由配置开关控制，默认关闭，
     * 只用于本地联调，行为是「以 mockSecret 开头 + 用户 ID」的令牌直接当成该用户。
     *
     * @param token          裸令牌
     * @param expectUserType 期望的登录端类型，可以为 null（不比对）
     * @return 登录用户
     */
    private LoginUser resolveLoginUser(String token, UserTypeEnum expectUserType) {
        try {
            return tokenService.validate(token, expectUserType);
        } catch (UnauthorizedException ex) {
            LoginUser mockUser = mockLoginUser(token, expectUserType);
            if (mockUser == null) {
                throw ex;
            }
            return mockUser;
        }
    }

    /**
     * 模拟登录：仅在开关打开、且请求能判定端类型、令牌以 mockSecret 开头时生效
     *
     * @param token          裸令牌
     * @param expectUserType 期望的登录端类型
     * @return 模拟的登录用户，不满足条件时返回 null
     */
    private LoginUser mockLoginUser(String token, UserTypeEnum expectUserType) {
        if (!securityProperties.isMockEnable() || expectUserType == null) {
            return null;
        }
        String mockSecret = securityProperties.getMockSecret();
        if (!StringUtils.hasText(mockSecret) || !token.startsWith(mockSecret)) {
            return null;
        }
        try {
            Long userId = Long.valueOf(token.substring(mockSecret.length()));
            log.warn("模拟登录生效：userId={}, userType={}，生产环境请关闭 zza.infra.security.mock-enable",
                    userId, expectUserType.getValue());
            return new LoginUser(userId, expectUserType.getValue(), "mock-user");
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
