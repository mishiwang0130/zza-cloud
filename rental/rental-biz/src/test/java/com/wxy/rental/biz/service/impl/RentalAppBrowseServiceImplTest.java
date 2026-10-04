package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.convert.RentalAppBrowseConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalBrowseHistoryMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalBrowseHistory;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.app.AppRoomBrowseRespVO;
import java.math.BigDecimal;
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
 * 用户端浏览记录单元测试：只查自己、批量回填房间与公寓信息、房间查不到时不让整页失败。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalAppBrowseServiceImplTest {

    /** 浏览记录 Mapper */
    @Mock
    private RentalBrowseHistoryMapper rentalBrowseHistoryMapper;

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

    /** App 浏览记录转换器 */
    @Mock
    private RentalAppBrowseConvert rentalAppBrowseConvert;

    /** 被测服务 */
    private RentalAppBrowseServiceImpl appBrowseService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appBrowseService = new RentalAppBrowseServiceImpl();
        ReflectionTestUtils.setField(appBrowseService, "rentalBrowseHistoryMapper", rentalBrowseHistoryMapper);
        ReflectionTestUtils.setField(appBrowseService, "rentalRoomMapper", rentalRoomMapper);
        ReflectionTestUtils.setField(appBrowseService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(appBrowseService, "rentalImageService", rentalImageService);
        ReflectionTestUtils.setField(appBrowseService, "rentalFileService", rentalFileService);
        ReflectionTestUtils.setField(appBrowseService, "rentalAppBrowseConvert", rentalAppBrowseConvert);
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
    @DisplayName("pageBrowse：未登录时抛未登录异常")
    void pageShouldRejectAnonymous() {
        assertThatThrownBy(() -> appBrowseService.pageBrowse(new PageReqVO()))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * 正常分页：批量回填房间号、租金、公寓名与封面图
     */
    @Test
    @DisplayName("pageBrowse：批量回填房间与公寓信息")
    void pageShouldFillRoomFields() {
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
        RentalBrowseHistory po = new RentalBrowseHistory();
        po.setId(1L);
        po.setUserId(100L);
        po.setRoomId(5L);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RentalBrowseHistory> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(po));
        page.setTotal(1L);
        when(rentalBrowseHistoryMapper.selectPage(any(), any())).thenReturn(page);
        AppRoomBrowseRespVO record = new AppRoomBrowseRespVO();
        record.setRoomId(5L);
        when(rentalAppBrowseConvert.toRespVOList(List.of(po))).thenReturn(List.of(record));
        RentalRoom room = new RentalRoom();
        room.setId(5L);
        room.setApartmentId(1L);
        room.setRoomNumber("301");
        room.setRent(new BigDecimal("2500.00"));
        when(rentalRoomMapper.selectBatchIds(List.of(5L))).thenReturn(List.of(room));
        RentalApartment apartment = new RentalApartment();
        apartment.setId(1L);
        apartment.setName("文三路公寓");
        when(rentalApartmentMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(apartment));
        when(rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.ROOM, List.of(5L)))
                .thenReturn(Map.of(5L, 88L));
        when(rentalFileService.getFileUrlMap(any())).thenReturn(Map.of(88L, "http://minio/cover"));

        PageRespVO<AppRoomBrowseRespVO> result = appBrowseService.pageBrowse(new PageReqVO());

        AppRoomBrowseRespVO filled = result.getRecords().get(0);
        assertThat(filled.getRoomNumber()).isEqualTo("301");
        assertThat(filled.getRent()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(filled.getApartmentId()).isEqualTo(1L);
        assertThat(filled.getApartmentName()).isEqualTo("文三路公寓");
        assertThat(filled.getCoverFileId()).isEqualTo(88L);
        assertThat(filled.getCoverFileUrl()).isEqualTo("http://minio/cover");
    }

    /**
     * 房间查不到时保留流水本身，只把回填字段留空
     */
    @Test
    @DisplayName("pageBrowse：房间查不到时不让整页失败")
    void pageShouldTolerateMissingRoom() {
        UserContextHolder.set(new LoginUser(100L, UserTypeEnum.APP.getValue(), "app-user"));
        RentalBrowseHistory po = new RentalBrowseHistory();
        po.setId(1L);
        po.setRoomId(5L);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RentalBrowseHistory> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(po));
        page.setTotal(1L);
        when(rentalBrowseHistoryMapper.selectPage(any(), any())).thenReturn(page);
        AppRoomBrowseRespVO record = new AppRoomBrowseRespVO();
        record.setRoomId(5L);
        when(rentalAppBrowseConvert.toRespVOList(List.of(po))).thenReturn(List.of(record));
        when(rentalRoomMapper.selectBatchIds(List.of(5L))).thenReturn(List.of());
        when(rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.ROOM, List.of(5L))).thenReturn(Map.of());
        when(rentalFileService.getFileUrlMap(any())).thenReturn(Map.of());

        PageRespVO<AppRoomBrowseRespVO> result = appBrowseService.pageBrowse(new PageReqVO());

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getRoomNumber()).isNull();
    }
}
