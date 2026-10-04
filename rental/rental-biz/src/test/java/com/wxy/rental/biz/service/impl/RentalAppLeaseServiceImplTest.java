package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalAppImageConvert;
import com.wxy.rental.biz.convert.RentalAppLeaseConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalLeaseStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalLeaseMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalLease;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.admin.ImageRespVO;
import com.wxy.rental.biz.vo.app.AppLeaseRespVO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 用户端租约单元测试：归属校验、三个动作的起始状态限制、合同地址回填。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalAppLeaseServiceImplTest {

    /** 租约 Mapper */
    @Mock
    private RentalLeaseMapper rentalLeaseMapper;

    /** 房间 Mapper */
    @Mock
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper */
    @Mock
    private RentalApartmentMapper rentalApartmentMapper;

    /** 图片服务 */
    @Mock
    private RentalImageService rentalImageService;

    /** 文件服务 */
    @Mock
    private RentalFileService rentalFileService;

    /** App 图片转换器 */
    @Mock
    private RentalAppImageConvert rentalAppImageConvert;

    /** App 租约转换器 */
    @Mock
    private RentalAppLeaseConvert rentalAppLeaseConvert;

    /** 被测服务 */
    private RentalAppLeaseServiceImpl appLeaseService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appLeaseService = new RentalAppLeaseServiceImpl();
        ReflectionTestUtils.setField(appLeaseService, "rentalLeaseMapper", rentalLeaseMapper);
        ReflectionTestUtils.setField(appLeaseService, "rentalRoomMapper", rentalRoomMapper);
        ReflectionTestUtils.setField(appLeaseService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(appLeaseService, "rentalImageService", rentalImageService);
        ReflectionTestUtils.setField(appLeaseService, "rentalFileService", rentalFileService);
        ReflectionTestUtils.setField(appLeaseService, "rentalAppImageConvert", rentalAppImageConvert);
        ReflectionTestUtils.setField(appLeaseService, "rentalAppLeaseConvert", rentalAppLeaseConvert);
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
    }

    /**
     * 清理登录上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 未登录时按未登录处理
     */
    @Test
    @DisplayName("pageLease：未登录时抛未登录异常")
    void pageShouldRejectAnonymous() {
        UserContextHolder.clear();

        assertThatThrownBy(() -> appLeaseService.pageLease(new PageReqVO()))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 详情：别人的租约按不存在处理
     */
    @Test
    @DisplayName("getLease：别人的租约按不存在处理")
    void detailShouldHideOtherUserLease() {
        when(rentalLeaseMapper.selectById(1L)).thenReturn(buildLease(1L, 999L, RentalLeaseStatusEnum.SIGNED));

        assertThatThrownBy(() -> appLeaseService.getLease(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.LEASE_NOT_FOUND.code()));
    }

    /**
     * 详情：回填公寓名、房间号、状态中文名与合同地址
     */
    @Test
    @DisplayName("getLease：回填公寓名、房间号、状态与合同地址")
    void detailShouldFillRelatedFields() {
        RentalLease lease = buildLease(1L, 100L, RentalLeaseStatusEnum.SIGNED);
        lease.setContractFileId(66L);
        when(rentalLeaseMapper.selectById(1L)).thenReturn(lease);
        AppLeaseRespVO respVO = new AppLeaseRespVO();
        when(rentalAppLeaseConvert.toRespVO(lease)).thenReturn(respVO);
        RentalRoom room = buildRoom(5L, 1L);
        when(rentalRoomMapper.selectBatchIds(List.of(5L))).thenReturn(List.of(room));
        RentalApartment apartment = new RentalApartment();
        apartment.setId(1L);
        apartment.setName("文三路公寓");
        when(rentalApartmentMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(apartment));
        when(rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.ROOM, List.of(5L)))
                .thenReturn(Map.of(5L, 88L));
        when(rentalFileService.getFileUrlMap(List.of(66L))).thenReturn(Map.of(66L, "http://minio/contract"));
        when(rentalImageService.listImages(RentalImageItemTypeEnum.ROOM, 5L))
                .thenReturn(List.of(new ImageRespVO()));
        when(rentalAppImageConvert.toAppImageRespVOList(any())).thenReturn(List.of());

        AppLeaseRespVO result = appLeaseService.getLease(1L);

        assertThat(result.getApartmentName()).isEqualTo("文三路公寓");
        assertThat(result.getRoomNumber()).isEqualTo("301");
        assertThat(result.getStatusName()).isEqualTo("已签约");
        assertThat(result.getCoverFileId()).isEqualTo(88L);
        assertThat(result.getContractFileUrl()).isEqualTo("http://minio/contract");
    }

    /**
     * 确认签约：只允许从「签约待确认」出发
     */
    @Test
    @DisplayName("confirm：已签约的租约不能再确认签约")
    void confirmShouldRejectNonPendingLease() {
        when(rentalLeaseMapper.selectById(1L)).thenReturn(buildLease(1L, 100L, RentalLeaseStatusEnum.SIGNED));

        assertThatThrownBy(() -> appLeaseService.confirm(1L))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode())
                        .isEqualTo(RentalErrorConstant.LEASE_STATUS_TRANSITION_INVALID.code()));
        verify(rentalLeaseMapper, never()).updateById(any(RentalLease.class));
    }

    /**
     * 确认签约：退租待确认（5→2 是运营驳回退租的动作）不能由用户本人触发
     */
    @Test
    @DisplayName("confirm：退租待确认的租约不能由用户确认签约")
    void confirmShouldRejectWithdrawPendingLease() {
        when(rentalLeaseMapper.selectById(1L)).thenReturn(buildLease(1L, 100L,
                RentalLeaseStatusEnum.WITHDRAW_PENDING));

        assertThatThrownBy(() -> appLeaseService.confirm(1L))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode())
                        .isEqualTo(RentalErrorConstant.LEASE_STATUS_TRANSITION_INVALID.code()));
    }

    /**
     * 确认签约：自己的待确认租约可以确认
     */
    @Test
    @DisplayName("confirm：签约待确认可以流转为已签约")
    void confirmShouldUpdateStatus() {
        RentalLease lease = buildLease(1L, 100L, RentalLeaseStatusEnum.PENDING_CONFIRM);
        when(rentalLeaseMapper.selectById(1L)).thenReturn(lease);

        appLeaseService.confirm(1L);

        assertThat(lease.getStatus()).isEqualTo(RentalLeaseStatusEnum.SIGNED.getValue());
        verify(rentalLeaseMapper).updateById(lease);
    }

    /**
     * 申请退租：已签约可以申请，其他状态不行
     */
    @Test
    @DisplayName("applyWithdraw：已签约可以申请退租，其他状态拒绝")
    void applyWithdrawShouldOnlyAllowSignedLease() {
        RentalLease signed = buildLease(1L, 100L, RentalLeaseStatusEnum.SIGNED);
        when(rentalLeaseMapper.selectById(1L)).thenReturn(signed);

        appLeaseService.applyWithdraw(1L);

        assertThat(signed.getStatus()).isEqualTo(RentalLeaseStatusEnum.WITHDRAW_PENDING.getValue());
        RentalLease dismissed = buildLease(2L, 100L, RentalLeaseStatusEnum.WITHDRAWN);
        when(rentalLeaseMapper.selectById(2L)).thenReturn(dismissed);
        assertThatThrownBy(() -> appLeaseService.applyWithdraw(2L))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode())
                        .isEqualTo(RentalErrorConstant.LEASE_STATUS_TRANSITION_INVALID.code()));
    }

    /**
     * 申请续约：已签约可以申请续约
     */
    @Test
    @DisplayName("applyRenew：已签约可以申请续约")
    void applyRenewShouldUpdateStatus() {
        RentalLease lease = buildLease(1L, 100L, RentalLeaseStatusEnum.SIGNED);
        when(rentalLeaseMapper.selectById(1L)).thenReturn(lease);

        appLeaseService.applyRenew(1L);

        assertThat(lease.getStatus()).isEqualTo(RentalLeaseStatusEnum.RENEW_PENDING.getValue());
    }

    /**
     * 构造租约实体
     *
     * @param id     租约 ID
     * @param userId 承租人 ID
     * @param status 租约状态
     * @return 租约实体
     */
    private RentalLease buildLease(Long id, Long userId, RentalLeaseStatusEnum status) {
        RentalLease lease = new RentalLease();
        lease.setId(id);
        lease.setUserId(userId);
        lease.setApartmentId(1L);
        lease.setRoomId(5L);
        lease.setStatus(status.getValue());
        return lease;
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
