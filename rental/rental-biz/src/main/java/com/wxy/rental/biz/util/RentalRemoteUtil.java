package com.wxy.rental.biz.util;

import com.wxy.common.core.exception.BizException;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import java.util.function.Supplier;

/**
 * 服务间调用工具：给 infra 的调用套一层统一的异常口径。
 *
 * <p>两类异常要分开对待：
 * <ul>
 *   <li>远端返回的业务失败（含熔断降级工厂返回的失败响应）——{@link BizException}，
 *       里面已经带着能解释原因的 code，原样抛出即可；</li>
 *   <li>其他运行时异常——连接不上、序列化失败、Sentinel 未生效导致异常直接外抛等，
 *       这些错到调用方是看不懂的，统一换成 {@link RentalErrorConstant#REMOTE_SERVICE_ERROR}，
 *       并把原始异常挂在 cause 上便于排查。</li>
 * </ul>
 *
 * @author wxy
 * @date 2026/10/04
 */
public final class RentalRemoteUtil {

    /**
     * 工具类，禁止实例化
     */
    private RentalRemoteUtil() {
    }

    /**
     * 执行一次服务间调用并按统一口径处理异常
     *
     * @param supplier 调用动作
     * @param action   调用说明，用于拼接错误提示（例如「查询字典」）
     * @param <T>      返回类型
     * @return 调用结果
     */
    public static <T> T call(Supplier<T> supplier, String action) {
        try {
            return supplier.get();
        } catch (BizException ex) {
            // 远端业务失败：code 是远端给的，比包一层「服务调用失败」更有信息量，原样抛出
            throw ex;
        } catch (RuntimeException ex) {
            throw new BizException(RentalErrorConstant.REMOTE_SERVICE_ERROR, action + "失败，请稍后重试", ex);
        }
    }
}
