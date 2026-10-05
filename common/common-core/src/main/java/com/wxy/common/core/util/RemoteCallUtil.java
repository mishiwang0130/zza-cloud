package com.wxy.common.core.util;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.ErrorCode;
import java.util.function.Supplier;

/**
 * 服务间调用工具：给「调别的服务」套一层统一的异常口径。
 *
 * <p>两类异常要分开对待：
 * <ul>
 *   <li>远端返回的业务失败（含熔断降级工厂返回的失败响应）——{@link BizException}，
 *       里面已经带着能解释原因的 code，原样抛出即可；</li>
 *   <li>其他运行时异常——连接不上、序列化失败、Sentinel 未生效导致异常直接外抛等，
 *       这些错到调用方是看不懂的，统一换成业务异常，并把原始异常挂在 cause 上便于排查。</li>
 * </ul>
 *
 * <p>与 common-feign 里 {@code FeignErrorDecoder} 的分工：解码器只管非 2xx 的 HTTP 响应，
 * 本工具兜住其余运行时异常以及 {@code requireData()} 抛出的失败响应，两者不是重复。
 *
 * <p>默认错误码是 {@link CommonErrorConstant#REMOTE_CALL_ERROR}；服务想把这类失败记到自己的
 * 错误码上（例如 {@code RentalErrorConstant.REMOTE_SERVICE_ERROR}），用带 {@code errorCode}
 * 参数的重载显式传入。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class RemoteCallUtil {

    /**
     * 工具类，禁止实例化
     */
    private RemoteCallUtil() {
    }

    /**
     * 执行一次服务间调用，失败时换成公共错误码
     *
     * @param supplier 调用动作
     * @param action   调用说明，用于拼接错误提示（例如「查询字典」）
     * @param <T>      返回类型
     * @return 调用结果
     */
    public static <T> T call(Supplier<T> supplier, String action) {
        return call(supplier, action, CommonErrorConstant.REMOTE_CALL_ERROR);
    }

    /**
     * 执行一次服务间调用，失败时换成指定的业务错误码
     *
     * @param supplier  调用动作
     * @param action    调用说明，用于拼接错误提示（例如「查询字典」）
     * @param errorCode 调用失败时使用的错误码
     * @param <T>       返回类型
     * @return 调用结果
     */
    public static <T> T call(Supplier<T> supplier, String action, ErrorCode errorCode) {
        try {
            return supplier.get();
        } catch (BizException ex) {
            // 远端业务失败：code 是远端给的，比包一层「服务调用失败」更有信息量，原样抛出
            throw ex;
        } catch (RuntimeException ex) {
            throw new BizException(errorCode, action + "失败，请稍后重试", ex);
        }
    }
}
