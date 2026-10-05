package com.wxy.rental.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.fallback.RentalViewAppointmentClientFallbackFactory;
import com.wxy.rental.api.constant.RentalApiConstant;
import com.wxy.rental.api.dto.ViewAppointmentRespDTO;
import com.wxy.rental.api.dto.ViewAppointmentCreateReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = RentalApiConstant.SERVICE_NAME, contextId = "rentalViewAppointmentClient",
        fallbackFactory = RentalViewAppointmentClientFallbackFactory.class)
public interface RentalViewAppointmentClient {

    @PostMapping("/internal-api/view-appointment/create")
    Result<Void> create(@RequestBody ViewAppointmentCreateReqDTO viewAppointmentCreateReqDTO);

    @GetMapping("/internal-api/view-appointment/get")
    Result<ViewAppointmentRespDTO> getByUserId(@RequestParam("userId") Long userId);

}
