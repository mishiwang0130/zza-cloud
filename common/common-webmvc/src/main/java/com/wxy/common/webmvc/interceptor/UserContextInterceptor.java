package com.wxy.common.webmvc.interceptor;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录上下文拦截器：把网关透传的用户请求头写入 {@link UserContextHolder}，请求结束时清理。
 *
 * <p>必须在 afterCompletion 清理：Tomcat 线程是复用的，ThreadLocal 不清理会让下一个请求
 * 读到上一个用户的身份，属于最危险的串号问题。
 *
 * <p>请求头缺失时不报错、不拦截：服务间内部调用、健康检查本来就没有登录用户，
 * 此时上下文为空，审计字段退化为系统用户 0。
 *
 * @author wxy
 * @date 2026/10/02
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {

    /**
     * 请求进入 Controller 前写入登录上下文
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  处理器
     * @return 恒为 true，不阻断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(HeaderConstant.USER_ID);
        if (!StringUtils.hasText(userId)) {
            return true;
        }
        try {
            UserContextHolder.set(new LoginUser(Long.valueOf(userId),
                    parseInteger(request.getHeader(HeaderConstant.USER_TYPE)),
                    request.getHeader(HeaderConstant.USER_NAME)));
        } catch (IllegalArgumentException ex) {
            // 请求头被伪造或格式错误时按未登录处理，避免脏数据污染上下文
            log.warn("解析登录用户请求头失败：userId={}", userId);
            UserContextHolder.clear();
        }
        return true;
    }

    /**
     * 请求结束后清理登录上下文，避免线程复用导致身份串号
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  处理器
     * @param ex       处理过程中的异常，正常结束时为 null
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    /**
     * 解析 Integer 类型的请求头
     *
     * @param value 请求头值
     * @return 解析结果，未携带时返回 null
     */
    private static Integer parseInteger(String value) {
        return StringUtils.hasText(value) ? Integer.valueOf(value) : null;
    }
}
