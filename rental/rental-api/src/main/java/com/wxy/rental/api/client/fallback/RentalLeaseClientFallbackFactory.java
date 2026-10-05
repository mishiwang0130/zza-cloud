package com.wxy.rental.api.client.fallback;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.RentalLeaseClient;
import com.wxy.rental.api.dto.LeaseRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RentalLeaseClientFallbackFactory implements FallbackFactory<RentalLeaseClient> {
    @Override
    public RentalLeaseClient create(Throwable cause) {
        return new RentalLeaseClient() {
            @Override
            public Result<LeaseRespDTO> getLeaseInfoByUserId(Long userId) {
                log.error("[getLeaseInfoByUserId][调用 rental服务 失败] cause={}", cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "rental服务暂时不可用，请稍后重试");
            }
        };
    }
}
