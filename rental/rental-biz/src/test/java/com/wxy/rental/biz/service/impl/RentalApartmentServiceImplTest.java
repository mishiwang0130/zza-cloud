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
import com.wxy.rental.biz.convert.RentalApartmentConvert;
import com.wxy.rental.biz.convert.RentalFeeItemConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalPaymentMethodEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentFeeMapper;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalFeeItemMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalApartmentFee;
import com.wxy.rental.biz.po.RentalFeeItem;
import com.wxy.rental.biz.service.RentalAreaService;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.admin.ApartmentCreateReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageItemRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentUpdatePublishStatusReqVO;
import com.wxy.rental.biz.vo.admin.DictItemVO;
import com.wxy.rental.biz.vo.admin.ImageRespVO;
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
 * 公寓服务单元测试：表单一次落全、详情按需回填、按市筛选与下架校验。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalApartmentServiceImplTest {

    /** 公寓 Mapper */
    @Mock
    private RentalApartmentMapper rentalApartmentMapper;

    /** 公寓费用项关联 Mapper */
    @Mock
    private RentalApartmentFeeMapper rentalApartmentFeeMapper;

    /** 费用项 Mapper */
    @Mock
    private RentalFeeItemMapper rentalFeeItemMapper;

    /** 公寓转换器 */
    @Mock
    private RentalApartmentConvert rentalApartmentConvert;

    /** 费用项转换器 */
    @Mock
    private RentalFeeItemConvert rentalFeeItemConvert;

    /** 字典服务 */
    @Mock
    private RentalDictService rentalDictService;

    /** 区划服务 */
    @Mock
    private RentalAreaService rentalAreaService;

    /** 图片服务 */
    @Mock
    private RentalImageService rentalImageService;

    /** 被测服务 */
    private RentalApartmentServiceImpl apartmentService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        apartmentService = new RentalApartmentServiceImpl();
        ReflectionTestUtils.setField(apartmentService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(apartmentService, "rentalApartmentFeeMapper", rentalApartmentFeeMapper);
        ReflectionTestUtils.setField(apartmentService, "rentalFeeItemMapper", rentalFeeItemMapper);
        ReflectionTestUtils.setField(apartmentService, "rentalApartmentConvert", rentalApartmentConvert);
        ReflectionTestUtils.setField(apartmentService, "rentalFeeItemConvert", rentalFeeItemConvert);
        ReflectionTestUtils.setField(apartmentService, "rentalDictService", rentalDictService);
        ReflectionTestUtils.setField(apartmentService, "rentalAreaService", rentalAreaService);
        ReflectionTestUtils.setField(apartmentService, "rentalImageService", rentalImageService);
    }

    /**
     * 新增：区县不存在时报「行政区划不存在」，不写库
     */
    @Test
    @DisplayName("createApartment：区县不存在时拒绝")
    void createShouldRejectUnknownDistrict() {
        ApartmentCreateReqVO reqVO = buildCreateReq();
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(java.util.Collections.singletonMap(3L, null));

        assertThatThrownBy(() -> apartmentService.createApartment(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.AREA_NOT_FOUND.code()));
        verify(rentalApartmentMapper, never()).insert(any(RentalApartment.class));
    }

    /**
     * 新增：主表落库后同时写费用项关联与图片，首次发布状态为未发布
     */
    @Test
    @DisplayName("createApartment：一次提交落主表、费用项与图片，默认未发布")
    void createShouldPersistWholeForm() {
        ApartmentCreateReqVO reqVO = buildCreateReq();
        reqVO.setFeeItemIds(List.of(7L));
        reqVO.setLabelCodes(List.of("near_subway"));
        reqVO.setImages(List.of());
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(Map.of(3L, "西湖区"));
        when(rentalDictService.joinCodes(any(), anyList())).thenReturn("near_subway");
        when(rentalApartmentMapper.insert(any(RentalApartment.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, RentalApartment.class).setId(100L);
            return 1;
        });
        when(rentalFeeItemMapper.selectBatchIds(List.of(7L))).thenReturn(List.of(new RentalFeeItem()));

        Long id = apartmentService.createApartment(reqVO);

        assertThat(id).isEqualTo(100L);
        ArgumentCaptor<RentalApartment> captor = ArgumentCaptor.forClass(RentalApartment.class);
        verify(rentalApartmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getPublishStatus())
                .isEqualTo(RentalPublishStatusEnum.UNPUBLISHED.getValue());
        assertThat(captor.getValue().getPaymentMethod())
                .isEqualTo(RentalPaymentMethodEnum.MONTHLY.getValue());
        verify(rentalApartmentFeeMapper).deleteByApartmentId(100L);
        verify(rentalApartmentFeeMapper).insert(any(RentalApartmentFee.class));
        verify(rentalImageService).replaceImages(eq(RentalImageItemTypeEnum.APARTMENT), eq(100L), anyList());
    }

    /**
     * 详情：区县名、付款方式中文名、标签、费用项与图片都要回填
     */
    @Test
    @DisplayName("getApartment：按需回填区县名、字典中文名、费用项与图片")
    void getApartmentShouldFillRelatedData() {
        RentalApartment po = new RentalApartment();
        po.setId(1L);
        po.setDistrictId(3L);
        po.setPaymentMethod(RentalPaymentMethodEnum.QUARTERLY.getValue());
        po.setLabelCodes("near_subway");
        when(rentalApartmentMapper.selectById(1L)).thenReturn(po);
        when(rentalApartmentConvert.toRespVO(po)).thenReturn(new ApartmentRespVO());
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(Map.of(3L, "西湖区"));
        when(rentalDictService.listDictItems(any(), eq("near_subway")))
                .thenReturn(List.of(new DictItemVO("近地铁", "near_subway")));
        when(rentalApartmentFeeMapper.selectList(any())).thenReturn(List.of());
        when(rentalImageService.listImages(RentalImageItemTypeEnum.APARTMENT, 1L))
                .thenReturn(List.of(new ImageRespVO()));

        ApartmentRespVO respVO = apartmentService.getApartment(1L);

        assertThat(respVO.getDistrictName()).isEqualTo("西湖区");
        assertThat(respVO.getPaymentMethodName()).isEqualTo("季付");
        assertThat(respVO.getLabelCodes()).extracting(DictItemVO::getValue).containsExactly("near_subway");
        assertThat(respVO.getImages()).hasSize(1);
        assertThat(respVO.getFeeItems()).isEmpty();
    }

    /**
     * 列表：按市筛选时先展开区县，再回填区县名
     */
    @Test
    @DisplayName("pageApartment：按市展开区县并回填区县名")
    void pageShouldExpandCityAndFillDistrictName() {
        ApartmentPageReqVO reqVO = new ApartmentPageReqVO();
        reqVO.setCityId(2L);
        ApartmentPageItemRespVO record = new ApartmentPageItemRespVO();
        record.setId(1L);
        record.setDistrictId(3L);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ApartmentPageItemRespVO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(record));
        page.setTotal(1L);
        when(rentalAreaService.listDistrictIdsByCity(2L)).thenReturn(List.of(3L));
        when(rentalApartmentMapper.selectApartmentPage(any(), eq(reqVO), eq(List.of(3L)))).thenReturn(page);
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(Map.of(3L, "西湖区"));

        PageRespVO<ApartmentPageItemRespVO> result = apartmentService.pageApartment(reqVO);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords().get(0).getDistrictName()).isEqualTo("西湖区");
    }

    /**
     * 列表：该市下没有区县时直接返回空页，不查库
     */
    @Test
    @DisplayName("pageApartment：市下没有区县时返回空页")
    void pageShouldReturnEmptyWhenCityHasNoDistrict() {
        ApartmentPageReqVO reqVO = new ApartmentPageReqVO();
        reqVO.setCityId(2L);
        when(rentalAreaService.listDistrictIdsByCity(2L)).thenReturn(List.of());

        PageRespVO<ApartmentPageItemRespVO> result = apartmentService.pageApartment(reqVO);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getRecords()).isEmpty();
        verify(rentalApartmentMapper, never()).selectApartmentPage(any(), any(), any());
    }

    /**
     * 下架：公寓下还有已发布房间时拒绝
     */
    @Test
    @DisplayName("updatePublishStatus：公寓下还有已发布房间时拒绝下架")
    void publishStatusShouldRejectWhenApartmentHasRoom() {
        RentalApartment po = new RentalApartment();
        po.setId(1L);
        when(rentalApartmentMapper.selectById(1L)).thenReturn(po);
        when(rentalApartmentMapper.countPublishedRooms(1L)).thenReturn(2L);
        ApartmentUpdatePublishStatusReqVO reqVO = new ApartmentUpdatePublishStatusReqVO();
        reqVO.setId(1L);
        reqVO.setPublishStatus(RentalPublishStatusEnum.UNPUBLISHED.getValue());

        assertThatThrownBy(() -> apartmentService.updatePublishStatus(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.APARTMENT_HAS_ROOM.code()));
        verify(rentalApartmentMapper, never()).updateById(any(RentalApartment.class));
    }

    /**
     * 上架：不校验房间（校验只在下架时做）
     */
    @Test
    @DisplayName("updatePublishStatus：上架时直接更新状态")
    void publishStatusShouldUpdateWhenPublishing() {
        RentalApartment po = new RentalApartment();
        po.setId(1L);
        when(rentalApartmentMapper.selectById(1L)).thenReturn(po);
        ApartmentUpdatePublishStatusReqVO reqVO = new ApartmentUpdatePublishStatusReqVO();
        reqVO.setId(1L);
        reqVO.setPublishStatus(RentalPublishStatusEnum.PUBLISHED.getValue());

        apartmentService.updatePublishStatus(reqVO);

        assertThat(po.getPublishStatus()).isEqualTo(RentalPublishStatusEnum.PUBLISHED.getValue());
        verify(rentalApartmentMapper).updateById(po);
    }

    /**
     * 构造公寓新增入参
     *
     * @return 新增入参
     */
    private ApartmentCreateReqVO buildCreateReq() {
        ApartmentCreateReqVO reqVO = new ApartmentCreateReqVO();
        reqVO.setName("文三路公寓");
        reqVO.setDistrictId(3L);
        return reqVO;
    }
}
