package com.wxy.rental.api.client;


import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.fallback.RentalLeaseClientFallbackFactory;
import com.wxy.rental.api.constant.RentalApiConstant;
import com.wxy.rental.api.dto.LeaseRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * @author wxy
 * @description 租赁客户
 * @date 2026/10/05
 */
@FeignClient(name = RentalApiConstant.SERVICE_NAME, contextId = "rentalLeaseClient",
        fallbackFactory = RentalLeaseClientFallbackFactory.class)
public interface RentalLeaseClient {

    /**
     * 按用户id获取租赁信息
     *
     * @param userId 用户ID
     * @return {@code Result<LeaseRespDTO> }
     * @author wxy
     * @date 2026/10/05
     */
    @PostMapping("/internal-api/lease/mine")
    Result<LeaseRespDTO> getLeaseInfoByUserId(@RequestParam("userId") Long userId);
}
