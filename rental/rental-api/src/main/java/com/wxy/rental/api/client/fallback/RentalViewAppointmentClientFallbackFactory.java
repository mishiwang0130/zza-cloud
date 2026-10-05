package com.wxy.rental.api.client.fallback;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.RentalLeaseClient;
import com.wxy.rental.api.client.RentalViewAppointmentClient;
import com.wxy.rental.api.dto.ViewAppointmentCreateReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RentalViewAppointmentClientFallbackFactory implements FallbackFactory<RentalViewAppointmentClient> {
    @Override
    public RentalViewAppointmentClient create(Throwable cause) {
        return new RentalViewAppointmentClient() {
            @Override
            public Result<Void> create(ViewAppointmentCreateReqDTO viewAppointmentCreateReqDTO) {
                log.error("[create][调用 view服务 失败] cause={}", cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "view服务暂时不可用，请稍后重试");
            }
        };
    }
}
