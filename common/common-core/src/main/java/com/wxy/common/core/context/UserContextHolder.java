package com.wxy.common.core.context;

import com.wxy.common.core.constant.CommonConstant;

/**
 * 登录用户上下文：以 ThreadLocal 保存当前线程正在操作的用户。
 *
 * <p>放在 common-core 而不是 common-security，是因为它同时被 Web 拦截器（写入）、
 * MyBatis 审计字段填充（读取）、Feign 拦截器（读取并透传）使用，而子模块之间不允许互相依赖。
 *
 * <p><b>使用约束</b>：只能保存当前线程的用户身份，不做跨线程传递；
 * Web 请求必须在 {@code afterCompletion} 中调用 {@link #clear()}，
 * 否则线程池复用时会读到上一个请求的用户（串号），这是最需要防范的坑。
 *
 * @author wxy
 * @date 2026/10/02
 */
public final class UserContextHolder {

    /** 当前线程的登录用户，未登录时为 null */
    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    /**
     * 工具类，禁止实例化
     */
    private UserContextHolder() {
    }

    /**
     * 写入当前登录用户
     *
     * @param loginUser 登录用户，可以为 null（等价于清空）
     */
    public static void set(LoginUser loginUser) {
        if (loginUser == null) {
            clear();
            return;
        }
        CONTEXT.set(loginUser);
    }

    /**
     * 获取当前登录用户
     *
     * @return 登录用户，未登录时返回 null
     */
    public static LoginUser get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID，未登录时返回 null
     */
    public static Long getUserId() {
        LoginUser loginUser = CONTEXT.get();
        return loginUser == null ? null : loginUser.userId();
    }

    /**
     * 获取当前登录用户 ID，未登录时用系统用户 ID 兜底
     *
     * <p>用于审计字段填充：未登录或系统内部调用时写入 {@code 0}，
     * 避免 {@code create_by} 出现 null 导致统计口径混乱。
     *
     * @return 用户 ID，未登录时返回 {@code 0}
     */
    public static Long getUserIdOrDefault() {
        Long userId = getUserId();
        return userId == null ? CommonConstant.SYSTEM_USER_ID : userId;
    }

    /**
     * 获取当前登录端类型
     *
     * @return 端类型，未登录时返回 null
     */
    public static Integer getUserType() {
        LoginUser loginUser = CONTEXT.get();
        return loginUser == null ? null : loginUser.userType();
    }

    /**
     * 清空当前线程的登录用户
     *
     * <p>请求结束、异步任务结束、线程归还线程池前必须调用。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
