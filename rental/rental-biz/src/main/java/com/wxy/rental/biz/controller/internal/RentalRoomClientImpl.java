package com.wxy.rental.biz.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.RentalRoomClient;
import com.wxy.rental.api.dto.RoomDetailDTO;
import com.wxy.rental.api.dto.RoomSearchReqDTO;
import com.wxy.rental.api.dto.RoomSummaryDTO;
import com.wxy.rental.biz.convert.RentalRoomConvert;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalAppRoomService;
import com.wxy.rental.biz.service.RentalRoomService;
import com.wxy.rental.biz.vo.app.AppRoomRespVO;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Hidden
@RestController
public class RentalRoomClientImpl implements RentalRoomClient {
    @Resource
    private RentalRoomService rentalRoomService;

    @Resource
    private RentalAppRoomService rentalAppRoomService;

    @Override
    public Result<List<RoomSummaryDTO>> searchAvailableRooms(RoomSearchReqDTO roomSearchReqDTO) {
        List<RoomSummaryDTO> roomSummaryDTOList = rentalRoomService.searchAvailableRooms(roomSearchReqDTO);
        return Result.success(roomSummaryDTOList);
    }

    @Override
    public Result<RoomDetailDTO> getRoomDetail(String roomNumber) {
        return Result.success(rentalAppRoomService.getRoomDetail(roomNumber));
    }
}
