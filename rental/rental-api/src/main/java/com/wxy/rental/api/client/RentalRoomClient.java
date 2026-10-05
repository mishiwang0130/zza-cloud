package com.wxy.rental.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.fallback.RentalRoomClientFallbackFactory;
import com.wxy.rental.api.constant.RentalApiConstant;
import com.wxy.rental.api.dto.RoomDetailDTO;
import com.wxy.rental.api.dto.RoomSearchReqDTO;
import com.wxy.rental.api.dto.RoomSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = RentalApiConstant.SERVICE_NAME, contextId = "rentalRoomClient",
        fallbackFactory = RentalRoomClientFallbackFactory.class)
public interface RentalRoomClient {
    @PostMapping("/internal-api/room/searchAvailableRooms")
    Result<List<RoomSummaryDTO>> searchAvailableRooms(@RequestBody RoomSearchReqDTO roomSearchReqDTO);

    @PostMapping("/internal-api/room/getDetail")
    Result<RoomDetailDTO> getRoomDetail(@RequestParam("roomNumber") String roomNumber);
}
