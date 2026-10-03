package com.wxy.infra.api.client.fallback;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraTokenClient;
import com.wxy.infra.api.dto.TokenCheckReqDTO;
import com.wxy.infra.api.dto.TokenCheckRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 凭证服务客户端的降级工厂：远程调用失败或被 Sentinel 熔断时触发。
 *
 * <p><b>降级一律拒绝</b>：返回「服务调用失败」的失败响应，绝不返回一个身份。
 * 鉴权是安全开关，依赖不可用时只能更保守，不能更宽松。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
@Component
public class InfraTokenClientFallbackFactory implements FallbackFactory<InfraTokenClient> {

    /**
     * 创建降级实现
     *
     * @param cause 触发降级的异常（调用失败或熔断）
     * @return 降级的客户端实现
     */
    @Override
    public InfraTokenClient create(Throwable cause) {
        return new InfraTokenClient() {

            /**
             * 降级处理：记录原因并返回失败响应
             *
             * @param reqDTO 校验入参
             * @return 失败的统一响应
             */
            @Override
            public Result<TokenCheckRespDTO> checkToken(TokenCheckReqDTO reqDTO) {
                log.error("[checkToken][调用 infra 校验令牌失败，按未登录处理] cause={}", cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "鉴权服务暂时不可用，请稍后重试");
            }
        };
    }
}
