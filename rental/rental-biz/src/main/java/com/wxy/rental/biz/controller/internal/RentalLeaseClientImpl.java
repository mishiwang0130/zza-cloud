package com.wxy.rental.biz.controller.internal;

import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.RentalLeaseClient;
import com.wxy.rental.api.dto.LeaseRespDTO;
import com.wxy.rental.biz.service.RentalLeaseService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class RentalLeaseClientImpl implements RentalLeaseClient {

    @Resource
    private RentalLeaseService rentalLeaseService;

    @Override
    public Result<LeaseRespDTO> getLeaseInfoByUserId(Long userId) {
        LeaseRespDTO leaseRespDTO = rentalLeaseService.getLeaseInfoByUserId(userId);
        return Result.success(leaseRespDTO);
    }
}
