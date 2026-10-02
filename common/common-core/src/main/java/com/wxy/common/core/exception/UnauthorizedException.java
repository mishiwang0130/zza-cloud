package com.wxy.common.core.exception;

import com.wxy.common.core.result.CommonErrorConstant;
import java.io.Serial;

/**
 * 未登录异常：未携带凭证、凭证失效或解析失败时抛出。
 *
 * <p>它是业务异常的语义特例——全局异常处理器会把它单独映射成 HTTP 401，
 * 其余业务异常仍返回 HTTP 200。
 *
 * @author wxy
 * @date 2026/10/02
 */
public class UnauthorizedException extends BizException {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用默认文案构造未登录异常
     */
    public UnauthorizedException() {
        super(CommonErrorConstant.UNAUTHORIZED);
    }

    /**
     * 用自定义文案构造未登录异常
     *
     * @param msg 自定义提示信息，可以为空
     */
    public UnauthorizedException(String msg) {
        super(CommonErrorConstant.UNAUTHORIZED, msg);
    }
}
