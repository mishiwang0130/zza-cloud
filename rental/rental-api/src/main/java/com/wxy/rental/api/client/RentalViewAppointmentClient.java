package com.wxy.rental.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.fallback.RentalRoomClientFallbackFactory;
import com.wxy.rental.api.client.fallback.RentalViewAppointmentClientFallbackFactory;
import com.wxy.rental.api.constant.RentalApiConstant;
import com.wxy.rental.api.dto.ViewAppointmentCreateReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = RentalApiConstant.SERVICE_NAME, contextId = "rentalViewAppointmentClient",
        fallbackFactory = RentalViewAppointmentClientFallbackFactory.class)
public interface RentalViewAppointmentClient {

    @GetMapping("/internal-api/view-appointment/create")
    Result<Void> create(@RequestBody ViewAppointmentCreateReqDTO viewAppointmentCreateReqDTO);

}
