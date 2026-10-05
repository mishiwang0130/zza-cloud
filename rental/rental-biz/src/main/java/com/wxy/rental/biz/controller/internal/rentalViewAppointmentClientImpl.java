package com.wxy.rental.biz.controller.internal;

import com.wxy.common.core.result.Result;
import com.wxy.rental.api.client.RentalViewAppointmentClient;
import com.wxy.rental.api.dto.ViewAppointmentRespDTO;
import com.wxy.rental.api.dto.ViewAppointmentCreateReqDTO;
import com.wxy.rental.biz.convert.RentalAppAppointmentConvert;
import com.wxy.rental.biz.service.RentalApartmentService;
import com.wxy.rental.biz.service.RentalAppAppointmentService;
import com.wxy.rental.biz.vo.admin.ApartmentRespVO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentCreateReqVO;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class rentalViewAppointmentClientImpl implements RentalViewAppointmentClient {
    @Resource
    private RentalAppAppointmentService rentalAppAppointmentService;
    @Resource
    private RentalAppAppointmentConvert rentalAppAppointmentConvert;
    @Resource
    private RentalApartmentService rentalApartmentService;
    @Override
    public Result<Void> create(ViewAppointmentCreateReqDTO viewAppointmentCreateReqDTO) {
        AppViewAppointmentCreateReqVO reqVO = rentalAppAppointmentConvert.toAppViewAppointmentCreateReqVO(viewAppointmentCreateReqDTO);
        ApartmentRespVO apartmentRespVO = rentalApartmentService.isExistByName(viewAppointmentCreateReqDTO.getApartmentName());
        reqVO.setApartmentId(apartmentRespVO.getId());
        rentalAppAppointmentService.createAppointment(reqVO);
        return Result.success();
    }

    @Override
    public Result<ViewAppointmentRespDTO> getByUserId(Long userId) {
        ViewAppointmentRespDTO viewDTO = rentalAppAppointmentService.getByUserId(userId);
        return Result.success(viewDTO);
    }
}
