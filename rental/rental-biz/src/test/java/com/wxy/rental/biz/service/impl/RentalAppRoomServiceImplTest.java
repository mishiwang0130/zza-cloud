package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalAppImageConvert;
import com.wxy.rental.biz.convert.RentalAppRoomConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.mq.producer.RentalBrowseHistoryProducer;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalAreaService;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.service.RentalFeeItemService;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.DictItemVO;
import com.wxy.rental.biz.vo.admin.ImageRespVO;
import com.wxy.rental.biz.vo.app.AppRoomItemRespVO;
import com.wxy.rental.biz.vo.app.AppRoomPageReqVO;
import com.wxy.rental.biz.vo.app.AppRoomRespVO;
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
 * 用户端房间查询单元测试：列表批量回填、详情补写浏览记录与上下架可见性。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalAppRoomServiceImplTest {

    /** 房间 Mapper */
    @Mock
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper */
    @Mock
    private RentalApartmentMapper rentalApartmentMapper;

    /** App 房间转换器 */
    @Mock
    private RentalAppRoomConvert rentalAppRoomConvert;

    /** App 图片转换器 */
    @Mock
    private RentalAppImageConvert rentalAppImageConvert;

    /** 区划服务 */
    @Mock
    private RentalAreaService rentalAreaService;

    /** 字典服务 */
    @Mock
    private RentalDictService rentalDictService;

    /** 图片服务 */
    @Mock
    private RentalImageService rentalImageService;

    /** 文件服务 */
    @Mock
    private RentalFileService rentalFileService;

    /** 费用项服务 */
    @Mock
    private RentalFeeItemService rentalFeeItemService;

    /** 浏览记录生产者 */
    @Mock
    private RentalBrowseHistoryProducer rentalBrowseHistoryProducer;

    /** 被测服务 */
    private RentalAppRoomServiceImpl appRoomService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appRoomService = new RentalAppRoomServiceImpl();
        ReflectionTestUtils.setField(appRoomService, "rentalRoomMapper", rentalRoomMapper);
        ReflectionTestUtils.setField(appRoomService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(appRoomService, "rentalAppRoomConvert", rentalAppRoomConvert);
        ReflectionTestUtils.setField(appRoomService, "rentalAppImageConvert", rentalAppImageConvert);
        ReflectionTestUtils.setField(appRoomService, "rentalAreaService", rentalAreaService);
        ReflectionTestUtils.setField(appRoomService, "rentalDictService", rentalDictService);
        ReflectionTestUtils.setField(appRoomService, "rentalImageService", rentalImageService);
        ReflectionTestUtils.setField(appRoomService, "rentalFileService", rentalFileService);
        ReflectionTestUtils.setField(appRoomService, "rentalFeeItemService", rentalFeeItemService);
        ReflectionTestUtils.setField(appRoomService, "rentalBrowseHistoryProducer", rentalBrowseHistoryProducer);
    }

    /**
     * 清理登录上下文，避免线程复用导致串号
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 列表：批量回填公寓名、区县名、朝向中文名、封面图与标签
     */
    @Test
    @DisplayName("pageRoom：批量回填列表展示字段")
    void pageShouldFillItemFields() {
        AppRoomPageReqVO reqVO = new AppRoomPageReqVO();
        RentalRoom po = buildRoom(5L, 1L);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RentalRoom> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(po));
        page.setTotal(1L);
        when(rentalRoomMapper.selectAppRoomPage(any(), eq(reqVO), eq(null))).thenReturn(page);
        AppRoomItemRespVO record = new AppRoomItemRespVO();
        when(rentalAppRoomConvert.toItemRespVOList(List.of(po))).thenReturn(List.of(record));
        when(rentalApartmentMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(buildApartment(1L)));
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(Map.of(3L, "西湖区"));
        when(rentalDictService.getLabelMap("rental_room_orientation")).thenReturn(Map.of("south", "朝南"));
        when(rentalDictService.listDictItemsBatch(any(), any()))
                .thenReturn(Map.of("master_room", List.of(new DictItemVO("主卧", "master_room"))));
        when(rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.ROOM, List.of(5L)))
                .thenReturn(Map.of(5L, 88L));
        when(rentalFileService.getFileUrlMap(any())).thenReturn(Map.of(88L, "http://minio/cover"));

        PageRespVO<AppRoomItemRespVO> result = appRoomService.pageRoom(reqVO);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(record.getApartmentName()).isEqualTo("文三路公寓");
        assertThat(record.getDistrictId()).isEqualTo(3L);
        assertThat(record.getDistrictName()).isEqualTo("西湖区");
        assertThat(record.getOrientationName()).isEqualTo("朝南");
        assertThat(record.getCoverFileId()).isEqualTo(88L);
        assertThat(record.getCoverFileUrl()).isEqualTo("http://minio/cover");
        assertThat(record.getDepositMonths()).isEqualTo(1);
        assertThat(record.getLabelCodes()).extracting(DictItemVO::getValue).containsExactly("master_room");
    }

    /**
     * 详情：未发布房间按不存在处理
     */
    @Test
    @DisplayName("getRoom：未发布房间报房间不存在")
    void detailShouldRejectUnpublishedRoom() {
        RentalRoom po = buildRoom(5L, 1L);
        po.setPublishStatus(RentalPublishStatusEnum.UNPUBLISHED.getValue());
        when(rentalRoomMapper.selectById(5L)).thenReturn(po);

        assertThatThrownBy(() -> appRoomService.getRoom(5L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.ROOM_NOT_FOUND.code()));
        verifyNoInteractions(rentalBrowseHistoryProducer);
    }

    /**
     * 列表：房间号关键字去空白、非法排序值按综合排序处理后再交给 SQL
     */
    @Test
    @DisplayName("pageRoom：空白关键字与非法排序值先归一化")
    void pageShouldNormalizeKeywordAndSortType() {
        AppRoomPageReqVO reqVO = new AppRoomPageReqVO();
        reqVO.setKeyword(" 301 ");
        reqVO.setSortType(7);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RentalRoom> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of());
        page.setTotal(0L);
        when(rentalRoomMapper.selectAppRoomPage(any(), eq(reqVO), eq(null))).thenReturn(page);

        PageRespVO<AppRoomItemRespVO> result = appRoomService.pageRoom(reqVO);

        assertThat(result.getRecords()).isEmpty();
        assertThat(reqVO.getKeyword()).isEqualTo("301");
        assertThat(reqVO.getSortType()).isZero();
    }

    /**
     * 详情：公寓已下架时房间也不再对外展示
     */
    @Test
    @DisplayName("getRoom：公寓已下架时报房间不存在")
    void detailShouldRejectRoomOfUnpublishedApartment() {
        when(rentalRoomMapper.selectById(5L)).thenReturn(buildRoom(5L, 1L));
        RentalApartment apartment = buildApartment(1L);
        apartment.setPublishStatus(RentalPublishStatusEnum.UNPUBLISHED.getValue());
        when(rentalApartmentMapper.selectById(1L)).thenReturn(apartment);

        assertThatThrownBy(() -> appRoomService.getRoom(5L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.ROOM_NOT_FOUND.code()));
    }

    /**
     * 详情：已登录时补写浏览记录并回填公寓信息与图片
     */
    @Test
    @DisplayName("getRoom：登录状态下发送浏览记录并回填详情字段")
    void detailShouldPublishBrowseHistoryWhenLoggedIn() {
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
        RentalRoom po = buildRoom(5L, 1L);
        when(rentalRoomMapper.selectById(5L)).thenReturn(po);
        RentalApartment apartment = buildApartment(1L);
        when(rentalApartmentMapper.selectById(1L)).thenReturn(apartment);
        AppRoomRespVO respVO = new AppRoomRespVO();
        when(rentalAppRoomConvert.toRespVO(po)).thenReturn(respVO);
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(Map.of(3L, "西湖区"));
        when(rentalDictService.getLabelMap("rental_room_orientation")).thenReturn(Map.of("south", "朝南"));
        when(rentalDictService.listDictItemsBatch(any(), any())).thenReturn(Map.of());
        when(rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.ROOM, List.of(5L))).thenReturn(Map.of());
        when(rentalFileService.getFileUrlMap(any())).thenReturn(Map.of());
        when(rentalFeeItemService.listByApartmentId(1L)).thenReturn(List.of());
        when(rentalImageService.listImages(RentalImageItemTypeEnum.ROOM, 5L)).thenReturn(List.of(new ImageRespVO()));
        when(rentalAppImageConvert.toAppImageRespVOList(any())).thenReturn(List.of());

        AppRoomRespVO result = appRoomService.getRoom(5L);

        assertThat(result.getAddressDetail()).isEqualTo("文三路 100 号");
        assertThat(result.getPhone()).isEqualTo("0571-88888888");
        assertThat(result.getIntroduction()).isEqualTo("近地铁");
        verify(rentalBrowseHistoryProducer).send(100L, 5L);
    }

    /**
     * 详情：未登录时不写浏览记录，其余照常返回
     */
    @Test
    @DisplayName("getRoom：未登录时不发送浏览记录")
    void detailShouldSkipBrowseHistoryWhenAnonymous() {
        when(rentalRoomMapper.selectById(5L)).thenReturn(buildRoom(5L, 1L));
        when(rentalApartmentMapper.selectById(1L)).thenReturn(buildApartment(1L));
        AppRoomRespVO respVO = new AppRoomRespVO();
        when(rentalAppRoomConvert.toRespVO(any())).thenReturn(respVO);
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(Map.of(3L, "西湖区"));
        when(rentalDictService.getLabelMap("rental_room_orientation")).thenReturn(Map.of());
        when(rentalDictService.listDictItemsBatch(any(), any())).thenReturn(Map.of());
        when(rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.ROOM, List.of(5L))).thenReturn(Map.of());
        when(rentalFileService.getFileUrlMap(any())).thenReturn(Map.of());
        when(rentalFeeItemService.listByApartmentId(1L)).thenReturn(List.of());
        when(rentalImageService.listImages(RentalImageItemTypeEnum.ROOM, 5L)).thenReturn(List.of());
        when(rentalAppImageConvert.toAppImageRespVOList(any())).thenReturn(List.of());

        appRoomService.getRoom(5L);

        verifyNoInteractions(rentalBrowseHistoryProducer);
    }

    /**
     * 构造已发布房间实体
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
        room.setOrientation("south");
        room.setPublishStatus(RentalPublishStatusEnum.PUBLISHED.getValue());
        room.setLabelCodes("master_room");
        room.setFacilityCodes("air_conditioner");
        return room;
    }

    /**
     * 构造已发布公寓实体
     *
     * @param apartmentId 公寓 ID
     * @return 公寓实体
     */
    private RentalApartment buildApartment(Long apartmentId) {
        RentalApartment apartment = new RentalApartment();
        apartment.setId(apartmentId);
        apartment.setName("文三路公寓");
        apartment.setDistrictId(3L);
        apartment.setDepositMonths(1);
        apartment.setPaymentMethod(1);
        apartment.setMinLeaseMonths(6);
        apartment.setAddressDetail("文三路 100 号");
        apartment.setPhone("0571-88888888");
        apartment.setIntroduction("近地铁");
        apartment.setPublishStatus(RentalPublishStatusEnum.PUBLISHED.getValue());
        return apartment;
    }
}
