package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.infra.api.dto.AppUserSimpleDTO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalLeaseConvert;
import com.wxy.rental.biz.enums.RentalLeaseSourceTypeEnum;
import com.wxy.rental.biz.enums.RentalLeaseStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalLeaseMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalLease;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalAppUserService;
import com.wxy.rental.biz.vo.admin.LeaseCreateReqVO;
import com.wxy.rental.biz.vo.admin.LeaseRespVO;
import com.wxy.rental.biz.vo.admin.LeaseUpdateReqVO;
import com.wxy.rental.biz.vo.admin.LeaseUpdateStatusReqVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 租约服务单元测试：签约校验、押金计算、条款保护与状态流转。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalLeaseServiceImplTest {

    /** 租约 Mapper */
    @Mock
    private RentalLeaseMapper rentalLeaseMapper;

    /** 房间 Mapper */
    @Mock
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper */
    @Mock
    private RentalApartmentMapper rentalApartmentMapper;

    /** 租约转换器 */
    @Mock
    private RentalLeaseConvert rentalLeaseConvert;

    /** 用户档案服务 */
    @Mock
    private RentalAppUserService rentalAppUserService;

    /** 被测服务 */
    private RentalLeaseServiceImpl leaseService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        leaseService = new RentalLeaseServiceImpl();
        ReflectionTestUtils.setField(leaseService, "rentalLeaseMapper", rentalLeaseMapper);
        ReflectionTestUtils.setField(leaseService, "rentalRoomMapper", rentalRoomMapper);
        ReflectionTestUtils.setField(leaseService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(leaseService, "rentalLeaseConvert", rentalLeaseConvert);
        ReflectionTestUtils.setField(leaseService, "rentalAppUserService", rentalAppUserService);
    }

    /**
     * 签约：房间不存在时拒绝
     */
    @Test
    @DisplayName("createLease：房间不存在时拒绝")
    void createShouldRejectUnknownRoom() {
        when(rentalRoomMapper.selectById(5L)).thenReturn(null);

        assertThatThrownBy(() -> leaseService.createLease(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.ROOM_NOT_FOUND.code()));
    }

    /**
     * 签约：房间不属于提交的公寓时拒绝
     */
    @Test
    @DisplayName("createLease：房间不属于该公寓时拒绝")
    void createShouldRejectApartmentMismatch() {
        when(rentalRoomMapper.selectById(5L)).thenReturn(buildRoom(5L, 99L));

        assertThatThrownBy(() -> leaseService.createLease(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.ROOM_APARTMENT_MISMATCH.code()));
    }

    /**
     * 签约：结束日期必须晚于开始日期
     */
    @Test
    @DisplayName("createLease：结束日期不晚于开始日期时拒绝")
    void createShouldRejectInvalidDateRange() {
        when(rentalRoomMapper.selectById(5L)).thenReturn(buildRoom(5L, 1L));
        LeaseCreateReqVO reqVO = buildCreateReq();
        reqVO.setLeaseEndDate(reqVO.getLeaseStartDate());

        assertThatThrownBy(() -> leaseService.createLease(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.LEASE_DATE_INVALID.code()));
    }

    /**
     * 签约：房间已有生效中的租约时拒绝
     */
    @Test
    @DisplayName("createLease：房间已有生效中租约时拒绝")
    void createShouldRejectRoomWithEffectiveLease() {
        when(rentalRoomMapper.selectById(5L)).thenReturn(buildRoom(5L, 1L));
        when(rentalRoomMapper.countEffectiveLeases(5L)).thenReturn(1L);

        assertThatThrownBy(() -> leaseService.createLease(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.ROOM_LEASE_EXISTS.code()));
    }

    /**
     * 签约：押金不传时按「租金 × 公寓押金月数」计算
     */
    @Test
    @DisplayName("createLease：押金不传时按租金 × 押金月数计算")
    void createShouldCalculateDeposit() {
        when(rentalRoomMapper.selectById(5L)).thenReturn(buildRoom(5L, 1L));
        when(rentalRoomMapper.countEffectiveLeases(5L)).thenReturn(0L);
        RentalApartment apartment = new RentalApartment();
        apartment.setId(1L);
        apartment.setDepositMonths(2);
        when(rentalApartmentMapper.selectById(1L)).thenReturn(apartment);
        when(rentalLeaseMapper.insert(any(RentalLease.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalLease.class).setId(88L);
            return 1;
        });

        Long id = leaseService.createLease(buildCreateReq());

        assertThat(id).isEqualTo(88L);
        ArgumentCaptor<RentalLease> captor = ArgumentCaptor.forClass(RentalLease.class);
        verify(rentalLeaseMapper).insert(captor.capture());
        assertThat(captor.getValue().getDeposit()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(captor.getValue().getStatus()).isEqualTo(RentalLeaseStatusEnum.PENDING_CONFIRM.getValue());
        assertThat(captor.getValue().getSourceType()).isEqualTo(RentalLeaseSourceTypeEnum.NEW.getValue());
    }

    /**
     * 状态流转：非法迁移拒绝
     */
    @Test
    @DisplayName("updateStatus：非法迁移拒绝")
    void updateStatusShouldRejectInvalidTransition() {
        RentalLease po = new RentalLease();
        po.setId(1L);
        po.setStatus(RentalLeaseStatusEnum.CANCELED.getValue());
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateStatusReqVO reqVO = new LeaseUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalLeaseStatusEnum.SIGNED.getValue());

        assertThatThrownBy(() -> leaseService.updateStatus(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode())
                                .isEqualTo(RentalErrorConstant.LEASE_STATUS_TRANSITION_INVALID.code()));
        verify(rentalLeaseMapper, never()).updateById(any(RentalLease.class));
    }

    /**
     * 状态流转：合法迁移更新状态
     */
    @Test
    @DisplayName("updateStatus：签约待确认可以流转为已签约")
    void updateStatusShouldAllowConfiguredTransition() {
        RentalLease po = new RentalLease();
        po.setId(1L);
        po.setStatus(RentalLeaseStatusEnum.PENDING_CONFIRM.getValue());
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateStatusReqVO reqVO = new LeaseUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalLeaseStatusEnum.SIGNED.getValue());

        leaseService.updateStatus(reqVO);

        assertThat(po.getStatus()).isEqualTo(RentalLeaseStatusEnum.SIGNED.getValue());
        verify(rentalLeaseMapper).updateById(po);
    }

    /**
     * 状态流转：确认退租时把租期结束日期改到指定退租日期
     */
    @Test
    @DisplayName("updateStatus：确认退租时按实际退租日期缩短租期")
    void updateStatusShouldShortenLeaseEndDateOnWithdraw() {
        RentalLease po = buildLease(RentalLeaseStatusEnum.WITHDRAW_PENDING);
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateStatusReqVO reqVO = new LeaseUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalLeaseStatusEnum.WITHDRAWN.getValue());
        reqVO.setWithdrawDate(LocalDate.of(2026, 6, 30));

        leaseService.updateStatus(reqVO);

        assertThat(po.getStatus()).isEqualTo(RentalLeaseStatusEnum.WITHDRAWN.getValue());
        assertThat(po.getLeaseEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        verify(rentalLeaseMapper).updateById(po);
    }

    /**
     * 状态流转：确认退租不传退租日期时按当天
     */
    @Test
    @DisplayName("updateStatus：确认退租不传退租日期时按当天")
    void updateStatusShouldDefaultWithdrawDateToToday() {
        // 原租期结束日取远期，保证「当天」必然落在租期内，断言不会随运行日期漂移
        RentalLease po = buildLease(RentalLeaseStatusEnum.WITHDRAW_PENDING,
                LocalDate.of(2026, 1, 1), LocalDate.of(2099, 12, 31));
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateStatusReqVO reqVO = new LeaseUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalLeaseStatusEnum.WITHDRAWN.getValue());

        leaseService.updateStatus(reqVO);

        assertThat(po.getLeaseEndDate()).isEqualTo(LocalDate.now());
        verify(rentalLeaseMapper).updateById(po);
    }

    /**
     * 状态流转：确认时原租期已过，按原租期结束日收口，不会把租期改长
     */
    @Test
    @DisplayName("updateStatus：确认退租时原租期已过则按原租期结束日收口")
    void updateStatusShouldNotExtendLeaseWhenOriginalTermEnded() {
        RentalLease po = buildLease(RentalLeaseStatusEnum.WITHDRAW_PENDING,
                LocalDate.of(1999, 1, 1), LocalDate.of(2000, 1, 1));
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateStatusReqVO reqVO = new LeaseUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalLeaseStatusEnum.WITHDRAWN.getValue());

        leaseService.updateStatus(reqVO);

        assertThat(po.getLeaseEndDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        verify(rentalLeaseMapper).updateById(po);
    }

    /**
     * 状态流转：退租日期超出原租期时拒绝
     */
    @Test
    @DisplayName("updateStatus：退租日期晚于原租期结束日期时拒绝")
    void updateStatusShouldRejectWithdrawDateAfterOriginalEnd() {
        RentalLease po = buildLease(RentalLeaseStatusEnum.WITHDRAW_PENDING);
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateStatusReqVO reqVO = new LeaseUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalLeaseStatusEnum.WITHDRAWN.getValue());
        reqVO.setWithdrawDate(LocalDate.of(2027, 1, 1));

        assertThatThrownBy(() -> leaseService.updateStatus(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode())
                                .isEqualTo(RentalErrorConstant.LEASE_WITHDRAW_DATE_INVALID.code()));
        verify(rentalLeaseMapper, never()).updateById(any(RentalLease.class));
    }

    /**
     * 状态流转：退租日期不晚于租约开始日期时拒绝
     */
    @Test
    @DisplayName("updateStatus：退租日期不晚于租约开始日期时拒绝")
    void updateStatusShouldRejectWithdrawDateNotAfterStart() {
        RentalLease po = buildLease(RentalLeaseStatusEnum.WITHDRAW_PENDING);
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateStatusReqVO reqVO = new LeaseUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalLeaseStatusEnum.WITHDRAWN.getValue());
        reqVO.setWithdrawDate(po.getLeaseStartDate());

        assertThatThrownBy(() -> leaseService.updateStatus(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode())
                                .isEqualTo(RentalErrorConstant.LEASE_WITHDRAW_DATE_INVALID.code()));
        verify(rentalLeaseMapper, never()).updateById(any(RentalLease.class));
    }

    /**
     * 状态流转：其他流转不动租期结束日期
     */
    @Test
    @DisplayName("updateStatus：驳回退租不改动租期结束日期")
    void updateStatusShouldKeepLeaseEndDateOnOtherTransition() {
        RentalLease po = buildLease(RentalLeaseStatusEnum.WITHDRAW_PENDING);
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateStatusReqVO reqVO = new LeaseUpdateStatusReqVO();
        reqVO.setId(1L);
        reqVO.setStatus(RentalLeaseStatusEnum.SIGNED.getValue());
        reqVO.setWithdrawDate(LocalDate.of(2026, 6, 30));

        leaseService.updateStatus(reqVO);

        assertThat(po.getLeaseEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        verify(rentalLeaseMapper).updateById(po);
    }

    /**
     * 条款维护：终态租约改条款时拒绝
     */
    @Test
    @DisplayName("updateLease：终态租约修改条款时拒绝")
    void updateShouldRejectTermChangeOnFinalStatus() {
        RentalLease po = new RentalLease();
        po.setId(1L);
        po.setStatus(RentalLeaseStatusEnum.CANCELED.getValue());
        po.setRent(new BigDecimal("2500.00"));
        po.setLeaseStartDate(LocalDate.of(2026, 1, 1));
        po.setLeaseEndDate(LocalDate.of(2026, 12, 31));
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateReqVO reqVO = new LeaseUpdateReqVO();
        reqVO.setId(1L);
        reqVO.setRent(new BigDecimal("2600.00"));

        assertThatThrownBy(() -> leaseService.updateLease(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.LEASE_UPDATE_FORBIDDEN.code()));
    }

    /**
     * 条款维护：终态租约提交原值不算改动，可以补合同文件与备注
     */
    @Test
    @DisplayName("updateLease：终态租约提交原值可补合同文件与备注")
    void updateShouldAllowSameTermsOnFinalStatus() {
        RentalLease po = new RentalLease();
        po.setId(1L);
        po.setStatus(RentalLeaseStatusEnum.EXPIRED.getValue());
        po.setRent(new BigDecimal("2500.00"));
        po.setLeaseStartDate(LocalDate.of(2026, 1, 1));
        po.setLeaseEndDate(LocalDate.of(2026, 12, 31));
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        LeaseUpdateReqVO reqVO = new LeaseUpdateReqVO();
        reqVO.setId(1L);
        reqVO.setRent(new BigDecimal("2500.00"));
        reqVO.setContractFileId(9L);
        reqVO.setRemark("已到期");

        leaseService.updateLease(reqVO);

        assertThat(po.getContractFileId()).isEqualTo(9L);
        assertThat(po.getRemark()).isEqualTo("已到期");
        verify(rentalLeaseMapper).updateById(po);
    }

    /**
     * 详情：回填公寓名、房间号与状态中文名（租客昵称待 infra 就绪）
     */
    @Test
    @DisplayName("getLease：回填公寓名、房间号、状态中文名与承租人信息")
    void getLeaseShouldFillRelatedData() {
        RentalLease po = new RentalLease();
        po.setId(1L);
        po.setUserId(100L);
        po.setApartmentId(1L);
        po.setRoomId(5L);
        po.setStatus(RentalLeaseStatusEnum.WITHDRAW_PENDING.getValue());
        when(rentalLeaseMapper.selectById(1L)).thenReturn(po);
        when(rentalLeaseConvert.toRespVO(po)).thenReturn(new LeaseRespVO());
        when(rentalAppUserService.getAppUserMap(List.of(100L)))
                .thenReturn(Map.of(100L, new AppUserSimpleDTO(100L, "小张", "13800001111")));
        RentalApartment apartment = new RentalApartment();
        apartment.setName("文三路公寓");
        when(rentalApartmentMapper.selectById(1L)).thenReturn(apartment);
        when(rentalRoomMapper.selectById(5L)).thenReturn(buildRoom(5L, 1L));

        LeaseRespVO respVO = leaseService.getLease(1L);

        assertThat(respVO.getApartmentName()).isEqualTo("文三路公寓");
        assertThat(respVO.getRoomNumber()).isEqualTo("301");
        assertThat(respVO.getStatusName()).isEqualTo("退租待确认");
        assertThat(respVO.getUserNickname()).isEqualTo("小张");
        assertThat(respVO.getUserMobile()).isEqualTo("13800001111");
    }

    /**
     * 构造指定状态的租约：租期 2026-01-01 ~ 2026-12-31
     *
     * @param status 租约状态
     * @return 租约实体
     */
    private RentalLease buildLease(RentalLeaseStatusEnum status) {
        return buildLease(status, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
    }

    /**
     * 构造指定状态与租期的租约
     *
     * @param status    租约状态
     * @param startDate 租约开始日期
     * @param endDate   租约结束日期
     * @return 租约实体
     */
    private RentalLease buildLease(RentalLeaseStatusEnum status, LocalDate startDate, LocalDate endDate) {
        RentalLease po = new RentalLease();
        po.setId(1L);
        po.setStatus(status.getValue());
        po.setLeaseStartDate(startDate);
        po.setLeaseEndDate(endDate);
        return po;
    }

    /**
     * 构造租约新增入参
     *
     * @return 新增入参
     */
    private LeaseCreateReqVO buildCreateReq() {
        LeaseCreateReqVO reqVO = new LeaseCreateReqVO();
        reqVO.setUserId(100L);
        reqVO.setApartmentId(1L);
        reqVO.setRoomId(5L);
        reqVO.setLeaseStartDate(LocalDate.of(2026, 1, 1));
        reqVO.setLeaseEndDate(LocalDate.of(2026, 12, 31));
        reqVO.setRent(new BigDecimal("2500.00"));
        return reqVO;
    }

    /**
     * 构造房间实体
     *
     * @param roomId      房间 ID
     * @param apartmentId 所属公寓 ID
     * @return 房间实体
     */
    private RentalRoom buildRoom(Long roomId, Long apartmentId) {
        RentalRoom room = new RentalRoom();
        room.setId(roomId);
        room.setApartmentId(apartmentId);
        room.setRoomNumber("301");
        return room;
    }
}
