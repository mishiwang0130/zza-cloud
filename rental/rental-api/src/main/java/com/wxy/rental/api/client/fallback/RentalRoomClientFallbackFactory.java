package com.wxy.rental.api.client.fallback;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.RentalRoomClient;
import com.wxy.rental.api.dto.RoomDetailDTO;
import com.wxy.rental.api.dto.RoomSearchReqDTO;
import com.wxy.rental.api.dto.RoomSummaryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RentalRoomClientFallbackFactory implements FallbackFactory<RentalRoomClient> {
    @Override
    public RentalRoomClient create(Throwable cause) {
        return new RentalRoomClient() {
            @Override
            public Result<List<RoomSummaryDTO>> searchAvailableRooms(RoomSearchReqDTO roomSearchReqDTO) {
                log.error("[searchAvailableRooms][调用 room服务 失败] cause={}", cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "room服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<RoomDetailDTO> getRoomDetail(String roomNumber) {
                log.error("[getRoomDetail][调用 room服务 失败] cause={}", cause.getMessage(), cause);
                return Result.error(CommonErrorConstant.REMOTE_CALL_ERROR, "room服务暂时不可用，请稍后重试");
            }
        };
    }
}
