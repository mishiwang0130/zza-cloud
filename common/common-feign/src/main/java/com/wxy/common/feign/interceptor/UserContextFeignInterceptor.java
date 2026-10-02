package com.wxy.common.feign.interceptor;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.StringUtils;

/**
 * Feign 登录上下文透传拦截器：把当前线程的登录用户写进下游请求头。
 *
 * <p>为什么必须透传：下游服务靠这些请求头识别操作人并填充审计字段，
 * 不透传就会出现审计记录张冠李戴的问题。
 *
 * <p>上下文为空（定时任务、启动初始化等）时不写任何头，下游按系统用户处理。
 *
 * @author wxy
 * @date 2026/10/02
 */
public class UserContextFeignInterceptor implements RequestInterceptor {

    /**
     * 把登录用户信息写入请求头
     *
     * @param template 待发送的请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        LoginUser loginUser = UserContextHolder.get();
        if (loginUser == null || loginUser.userId() == null) {
            return;
        }
        template.header(HeaderConstant.USER_ID, String.valueOf(loginUser.userId()));
        if (loginUser.userType() != null) {
            template.header(HeaderConstant.USER_TYPE, String.valueOf(loginUser.userType()));
        }
        if (StringUtils.hasText(loginUser.username())) {
            template.header(HeaderConstant.USER_NAME, loginUser.username());
        }
    }
}
