package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalViewAppointmentConvert;
import com.wxy.rental.biz.enums.RentalAppointmentStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalViewAppointmentMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalViewAppointment;
import com.wxy.rental.biz.vo.admin.ViewAppointmentPageReqVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentRespVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentUpdateStatusReqVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 看房预约服务单元测试：列表批量回填公寓名、状态流转限制。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalViewAppointmentServiceImplTest {

    /** 预约 Mapper */
    @Mock
    private RentalViewAppointmentMapper rentalViewAppointmentMapper;

    /** 公寓 Mapper */
    @Mock
    private RentalApartmentMapper rentalApartmentMapper;

    /** 预约转换器 */
    @Mock
    private RentalViewAppointmentConvert rentalViewAppointmentConvert;

    /** 被测服务 */
    private RentalViewAppointmentServiceImpl appointmentService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appointmentService = new RentalViewAppointmentServiceImpl();
        ReflectionTestUtils.setField(appointmentService, "rentalViewAppointmentMapper", rentalViewAppointmentMapper);
        ReflectionTestUtils.setField(appointmentService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(appointmentService, "rentalViewAppointmentConvert", rentalViewAppointmentConvert);
    }

    /**
     * 列表：批量回填公寓名与状态中文名，租客昵称留空（等 infra）
     */
    @Test
    @DisplayName("pageAppointment：批量回填公寓名与状态中文名")
    void pageShouldFillApartmentNameAndStatusName() {
        ViewAppointmentPageReqVO reqVO = new ViewAppointmentPageReqVO();
        RentalViewAppointment po = new RentalViewAppointment();
        po.setId(1L);
        po.setApartmentId(1L);
        po.setStatus(RentalAppointmentStatusEnum.PENDING.getValue());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RentalViewAppointment> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(po));
        page.setTotal(1L);
        when(rentalViewAppointmentMapper.selectAppointmentPage(any(), any())).thenReturn(page);
        ViewAppointmentRespVO record = new ViewAppointmentRespVO();
        record.setApartmentId(1L);
        record.setStatus(RentalAppointmentStatusEnum.PENDING.getValue());
        when(rentalViewAppointmentConvert.toRespVOList(List.of(po))).thenReturn(List.of(record));
        RentalApartment apartment = new RentalApartment();
        apartment.setId(1L);
        apartment.setName("文三路公寓");
        when(rentalApartmentMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(apartment));

        PageRespVO<ViewAppointmentRespVO> result = appointmentService.pageAppointment(reqVO);

        assertThat(result.getRecords().get(0).getApartmentName()).isEqualTo("文三路公寓");
        assertThat(result.getRecords().get(0).getStatusName()).isEqualTo("待看房");
        assertThat(result.getRecords().get(0).getUserNickname()).isNull();
    }

    /**
     * 状态流转：待看房可以取消
     */
    @Test
    @DisplayName("updateStatus：待看房可以流转为已取消")
    void updateStatusShouldAllowCancel() {
        RentalViewAppointment po = new RentalViewAppointment();
        po.setId(1L);
        po.setStatus(RentalAppointmentStatusEnum.PENDING.getValue());
        when(rentalViewAppointmentMapper.selectById(1L)).thenReturn(po);
        ViewAppointmentUpdateStatusReqVO reqVO = new ViewAppointmentUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalAppointmentStatusEnum.CANCELED.getValue());

        appointmentService.updateStatus(reqVO);

        assertThat(po.getStatus()).isEqualTo(RentalAppointmentStatusEnum.CANCELED.getValue());
        verify(rentalViewAppointmentMapper).updateById(po);
    }

    /**
     * 状态流转：终态不允许再流转
     */
    @Test
    @DisplayName("updateStatus：终态流转时拒绝")
    void updateStatusShouldRejectFinalStatusTransition() {
        RentalViewAppointment po = new RentalViewAppointment();
        po.setId(1L);
        po.setStatus(RentalAppointmentStatusEnum.VIEWED.getValue());
        when(rentalViewAppointmentMapper.selectById(1L)).thenReturn(po);
        ViewAppointmentUpdateStatusReqVO reqVO = new ViewAppointmentUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalAppointmentStatusEnum.CANCELED.getValue());

        assertThatThrownBy(() -> appointmentService.updateStatus(reqVO))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode())
                        .isEqualTo(RentalErrorConstant.APPOINTMENT_STATUS_TRANSITION_INVALID.code()));
        verify(rentalViewAppointmentMapper, never()).updateById(any(RentalViewAppointment.class));
    }
}
