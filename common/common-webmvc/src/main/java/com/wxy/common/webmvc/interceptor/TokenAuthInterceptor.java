package com.wxy.common.webmvc.interceptor;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.security.TokenValidator;
import com.wxy.common.webmvc.config.SecurityProperties;
import com.wxy.common.webmvc.config.WebProperties;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录凭证拦截器：校验访问令牌并把登录用户写入上下文，所有业务服务共用。
 *
 * <p>职责分四步：
 * <ol>
 *   <li>免登录的直接放行：接口标注 {@link PermitAll}，或路径命中 {@code zza.security.permit-all-urls}；
 *       放行前仍会尝试用令牌还原身份（「可选登录」），令牌缺失或无效才降级为匿名；</li>
 *   <li>取令牌：优先 {@code Authorization} 头，其次 {@code ?token=} 请求参数（WebSocket、SSE 场景）；</li>
 *   <li>校验令牌：交给各服务自己的 {@link TokenValidator} 实现（本类不认识用户表、Redis 与数据库）；</li>
 *   <li>比对端类型（按接口前缀）后写入 {@link UserContextHolder}。</li>
 * </ol>
 *
 * <p><b>免登录接口是「可选登录」而不是「一定匿名」</b>：访客能匿名访问它们，登录用户同样会访问
 * （App 房间详情顺带补写浏览记录就是这种场景），所以带上有效令牌时要照常把身份写进上下文，
 * 让业务代码知道「谁在操作」；令牌缺失、过期或依赖不可用时只降级成匿名，不能反过来把免登录接口挡掉。
 * 上下文里已有的身份（服务间调用按 {@code X-User-Id} 头还原的那份）在降级时保留，不清空。
 *
 * <p><b>默认要求登录</b>：拦截器注册在所有路径上，没有显式声明免登录的请求都必须带有效令牌，
 * 安全相关的默认值是「拒绝」。同理，端类型判定只认约定前缀（{@code /admin-api} → 管理后台、
 * {@code /app-api} → 用户端），判不出来时不做比对，而不是兜底成某一端。
 *
 * <p>顺序上排在 {@link UserContextInterceptor} 之后：common 的上下文拦截器会按请求头还原身份，
 * 本拦截器再用「令牌解析出的身份」覆盖它，保证需要登录的接口以令牌为准（请求头只能来自网关或上游服务，
 * 但令牌才是权威）；只有免登录接口才会沿用请求头带来的身份。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
public class TokenAuthInterceptor implements HandlerInterceptor {

    /** 白名单路径匹配器：白名单是 Ant 风格模式 */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 令牌校验器：由各服务提供实现 */
    private final TokenValidator tokenValidator;

    /** Web 层配置：提供端前缀，用于判定请求属于哪一端 */
    private final WebProperties webProperties;

    /** 鉴权配置：令牌参数名、免登录白名单与模拟登录开关 */
    private final SecurityProperties securityProperties;

    /**
     * 构造拦截器
     *
     * @param tokenValidator     令牌校验器
     * @param webProperties      Web 层配置
     * @param securityProperties 鉴权配置
     */
    public TokenAuthInterceptor(TokenValidator tokenValidator, WebProperties webProperties,
                                SecurityProperties securityProperties) {
        this.tokenValidator = tokenValidator;
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
        boolean permitAll = isPermitAll(handler) || isPermitAllUrl(request);
        String token = resolveToken(request);
        if (permitAll) {
            // 免登录接口也要尽量还原身份：拿到身份时写入上下文，拿不到时保持原样（可能是请求头透传的服务间身份）并放行
            LoginUser loginUser = validateQuietly(token);
            if (loginUser != null) {
                UserContextHolder.set(loginUser);
            }
            return true;
        }
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
     * 判断请求路径是否命中免登录白名单
     *
     * @param request 当前请求
     * @return 命中任一条白名单模式时返回 true
     */
    private boolean isPermitAllUrl(HttpServletRequest request) {
        List<String> permitAllUrls = securityProperties.getPermitAllUrls();
        if (permitAllUrls == null || permitAllUrls.isEmpty()) {
            return false;
        }
        String uri = request.getRequestURI();
        for (String pattern : permitAllUrls) {
            if (StringUtils.hasText(pattern) && PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求中取访问令牌
     *
     * <p>优先标准请求头，其次请求参数（WebSocket、SSE 无法设置请求头）。
     *
     * @param request 当前请求
     * @return 裸令牌（已去掉 Bearer 前缀），没有携带时返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HeaderConstant.AUTHORIZATION);
        String token = stripBearer(authorization);
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
     * 去掉 {@code Authorization} 头里的 {@code Bearer} 前缀
     *
     * @param authorization 请求头原值，可以为 null
     * @return 裸令牌，入参为空时返回 null
     */
    private String stripBearer(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String prefix = "Bearer ";
        return authorization.startsWith(prefix)
                ? authorization.substring(prefix.length()).trim()
                : authorization.trim();
    }

    /**
     * 按接口前缀判定请求属于哪一端
     *
     * <p>判不出来返回 null，表示不比对端类型：令牌本身仍必须有效，只是不再校验它是哪一端签发的。
     * 这样没有端前缀的接口（服务内部接口、将来的 WebSocket 连接地址）不会被误判成某一端。
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
     * 校验令牌并比对端类型
     *
     * <p>真实校验失败后才会尝试模拟登录：模拟登录由配置开关控制，默认关闭，只用于本地联调。
     *
     * @param token          裸令牌
     * @param expectUserType 期望的登录端类型，可以为 null（不比对）
     * @return 登录用户
     */
    private LoginUser resolveLoginUser(String token, UserTypeEnum expectUserType) {
        LoginUser loginUser;
        try {
            loginUser = tokenValidator.validate(token);
        } catch (UnauthorizedException ex) {
            loginUser = mockLoginUser(token, expectUserType);
            if (loginUser == null) {
                throw ex;
            }
        }
        assertUserType(loginUser, expectUserType);
        return loginUser;
    }

    /**
     * 尝试用令牌还原登录用户，失败一律按匿名处理
     *
     * <p>只给免登录接口用（「可选登录」）：这些接口对访客开放，令牌可能压根没带，
     * 也可能已过期而前端还没刷新，两种情况都必须放行，所以校验失败只记日志不抛出。
     *
     * <p>这里刻意不做端类型比对：免登录接口对所有人开放，能解析出身份就说明令牌是真的，
     * 不属于「拿另一种端的令牌越权访问受保护接口」那种需要拦下的场景。
     *
     * <p>返回 null 时调用方不能顺手清空上下文，否则会把请求头还原的服务间身份一起清掉。
     *
     * @param token 裸令牌，可以为 null（未携带令牌）
     * @return 登录用户，未携带令牌、令牌无效或校验依赖不可用时返回 null
     */
    private LoginUser validateQuietly(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            return tokenValidator.validate(token);
        } catch (RuntimeException ex) {
            log.debug("[validateQuietly][免登录接口携带的令牌无效，按匿名放行] error={}", ex.getMessage());
            return null;
        }
    }

    /**
     * 校验登录用户是不是本端签发的
     *
     * <p>登录用户的端类型为 null 表示该实现不区分端（例如放行实现），此时跳过比对；
     * infra 这类真的区分两端的服务，实现会返回真实端类型，比对照常生效。
     *
     * @param loginUser      登录用户
     * @param expectUserType 期望的登录端类型，可以为 null（不比对）
     */
    private void assertUserType(LoginUser loginUser, UserTypeEnum expectUserType) {
        if (expectUserType == null || loginUser.userType() == null
                || expectUserType.getValue().equals(loginUser.userType())) {
            return;
        }
        throw new UnauthorizedException("登录端类型不匹配，请使用对应端的登录入口");
    }

    /**
     * 模拟登录：仅在开关打开、能判定端类型、且令牌以 mockSecret 开头时生效
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
            log.warn("模拟登录生效：userId={}, userType={}，生产环境请关闭 zza.security.mock-enable",
                    userId, expectUserType.getValue());
            return new LoginUser(userId, expectUserType.getValue(), "mock-user");
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
