package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalRoomConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.admin.RoomCreateReqVO;
import com.wxy.rental.biz.vo.admin.RoomPageItemRespVO;
import com.wxy.rental.biz.vo.admin.RoomPageReqVO;
import com.wxy.rental.biz.vo.admin.RoomSimpleRespVO;
import com.wxy.rental.biz.vo.admin.RoomUpdatePublishStatusReqVO;
import java.math.BigDecimal;
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
 * 房间服务单元测试：房间号唯一、默认未发布、入住状态派生与下架校验。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalRoomServiceImplTest {

    /** 房间 Mapper */
    @Mock
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper */
    @Mock
    private RentalApartmentMapper rentalApartmentMapper;

    /** 房间转换器 */
    @Mock
    private RentalRoomConvert rentalRoomConvert;

    /** 字典服务 */
    @Mock
    private RentalDictService rentalDictService;

    /** 图片服务 */
    @Mock
    private RentalImageService rentalImageService;

    /** 被测服务 */
    private RentalRoomServiceImpl roomService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        roomService = new RentalRoomServiceImpl();
        ReflectionTestUtils.setField(roomService, "rentalRoomMapper", rentalRoomMapper);
        ReflectionTestUtils.setField(roomService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(roomService, "rentalRoomConvert", rentalRoomConvert);
        ReflectionTestUtils.setField(roomService, "rentalDictService", rentalDictService);
        ReflectionTestUtils.setField(roomService, "rentalImageService", rentalImageService);
    }

    /**
     * 新增：所属公寓不存在时拒绝
     */
    @Test
    @DisplayName("createRoom：所属公寓不存在时拒绝")
    void createShouldRejectUnknownApartment() {
        when(rentalApartmentMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> roomService.createRoom(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.APARTMENT_NOT_FOUND.code()));
        verify(rentalRoomMapper, never()).insert(any(RentalRoom.class));
    }

    /**
     * 新增：同一公寓下房间号重复时拒绝
     */
    @Test
    @DisplayName("createRoom：房间号重复时拒绝")
    void createShouldRejectDuplicateRoomNumber() {
        when(rentalApartmentMapper.selectById(1L)).thenReturn(new RentalApartment());
        when(rentalRoomMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> roomService.createRoom(buildCreateReq()))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.ROOM_NUMBER_EXISTS.code()));
    }

    /**
     * 新增：成功时默认未发布，并写图片
     */
    @Test
    @DisplayName("createRoom：默认未发布并随表单一并写图片")
    void createShouldPersistRoomAndImages() {
        when(rentalApartmentMapper.selectById(1L)).thenReturn(new RentalApartment());
        when(rentalRoomMapper.selectCount(any())).thenReturn(0L);
        // 第二个入参可能是 null（本次没提交标签 / 配套），用 any() 而不是 anyList()
        when(rentalDictService.joinCodes(any(), any())).thenReturn("");
        when(rentalRoomMapper.insert(any(RentalRoom.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalRoom.class).setId(50L);
            return 1;
        });

        Long id = roomService.createRoom(buildCreateReq());

        assertThat(id).isEqualTo(50L);
        ArgumentCaptor<RentalRoom> captor = ArgumentCaptor.forClass(RentalRoom.class);
        verify(rentalRoomMapper).insert(captor.capture());
        assertThat(captor.getValue().getPublishStatus())
                .isEqualTo(RentalPublishStatusEnum.UNPUBLISHED.getValue());
        verify(rentalImageService).replaceImages(eq(RentalImageItemTypeEnum.ROOM), eq(50L), anyList());
    }

    /**
     * 列表：入住状态由 SQL 派生，朝向中文名由字典回填
     */
    @Test
    @DisplayName("pageRoom：回填朝向中文名")
    void pageShouldFillOrientationName() {
        RoomPageReqVO reqVO = new RoomPageReqVO();
        RoomPageItemRespVO record = new RoomPageItemRespVO();
        record.setId(1L);
        record.setOrientation("south");
        record.setCheckInStatus(1);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RoomPageItemRespVO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(record));
        page.setTotal(1L);
        when(rentalRoomMapper.selectRoomPage(any(), eq(reqVO))).thenReturn(page);
        when(rentalDictService.getLabelMap("rental_room_orientation")).thenReturn(Map.of("south", "朝南"));

        PageRespVO<RoomPageItemRespVO> result = roomService.pageRoom(reqVO);

        assertThat(result.getRecords().get(0).getOrientationName()).isEqualTo("朝南");
        assertThat(result.getRecords().get(0).getCheckInStatus()).isEqualTo(1);
    }

    /**
     * 下架：房间存在生效中的租约时拒绝
     */
    @Test
    @DisplayName("updatePublishStatus：房间有生效中租约时拒绝下架")
    void publishStatusShouldRejectWhenRoomHasLease() {
        RentalRoom po = new RentalRoom();
        po.setId(1L);
        when(rentalRoomMapper.selectById(1L)).thenReturn(po);
        when(rentalRoomMapper.countEffectiveLeases(1L)).thenReturn(1L);
        RoomUpdatePublishStatusReqVO reqVO = new RoomUpdatePublishStatusReqVO();
        reqVO.setId(1L);
        reqVO.setPublishStatus(RentalPublishStatusEnum.UNPUBLISHED.getValue());

        assertThatThrownBy(() -> roomService.updatePublishStatus(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.ROOM_HAS_LEASE.code()));
        verify(rentalRoomMapper, never()).updateById(any(RentalRoom.class));
    }

    /**
     * 选房下拉：按公寓查房间精简信息
     */
    @Test
    @DisplayName("listSimpleByApartment：按公寓返回精简列表")
    void listSimpleShouldReturnConvertedList() {
        RentalRoom po = new RentalRoom();
        when(rentalRoomMapper.selectList(any())).thenReturn(List.of(po));
        RoomSimpleRespVO respVO = new RoomSimpleRespVO();
        respVO.setRoomNumber("301");
        when(rentalRoomConvert.toSimpleRespVOList(List.of(po))).thenReturn(List.of(respVO));

        List<RoomSimpleRespVO> rooms = roomService.listSimpleByApartment(1L);

        assertThat(rooms).extracting(RoomSimpleRespVO::getRoomNumber).containsExactly("301");
        assertThat(roomService.listSimpleByApartment(null)).isEmpty();
    }

    /**
     * 构造房间新增入参
     *
     * @return 新增入参
     */
    private RoomCreateReqVO buildCreateReq() {
        RoomCreateReqVO reqVO = new RoomCreateReqVO();
        reqVO.setApartmentId(1L);
        reqVO.setRoomNumber("301");
        reqVO.setRent(new BigDecimal("2500"));
        reqVO.setImages(List.of());
        return reqVO;
    }
}
