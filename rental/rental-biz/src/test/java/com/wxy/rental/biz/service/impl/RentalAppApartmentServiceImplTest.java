package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.rental.biz.bo.ApartmentMinRentBO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalAppApartmentConvert;
import com.wxy.rental.biz.convert.RentalAppImageConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.service.RentalAreaService;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.service.RentalFeeItemService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.DictItemVO;
import com.wxy.rental.biz.vo.admin.ImageRespVO;
import com.wxy.rental.biz.vo.app.AppApartmentItemRespVO;
import com.wxy.rental.biz.vo.app.AppApartmentPageReqVO;
import com.wxy.rental.biz.vo.app.AppApartmentRespVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 用户端公寓查询单元测试：只查已发布、按市展开区县、批量回填展示字段。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalAppApartmentServiceImplTest {

    /** 公寓 Mapper */
    @Mock
    private RentalApartmentMapper rentalApartmentMapper;

    /** App 公寓转换器 */
    @Mock
    private RentalAppApartmentConvert rentalAppApartmentConvert;

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

    /** 费用项服务 */
    @Mock
    private RentalFeeItemService rentalFeeItemService;

    /** 被测服务 */
    private RentalAppApartmentServiceImpl appApartmentService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        appApartmentService = new RentalAppApartmentServiceImpl();
        ReflectionTestUtils.setField(appApartmentService, "rentalApartmentMapper", rentalApartmentMapper);
        ReflectionTestUtils.setField(appApartmentService, "rentalAppApartmentConvert", rentalAppApartmentConvert);
        ReflectionTestUtils.setField(appApartmentService, "rentalAppImageConvert", rentalAppImageConvert);
        ReflectionTestUtils.setField(appApartmentService, "rentalAreaService", rentalAreaService);
        ReflectionTestUtils.setField(appApartmentService, "rentalDictService", rentalDictService);
        ReflectionTestUtils.setField(appApartmentService, "rentalImageService", rentalImageService);
        ReflectionTestUtils.setField(appApartmentService, "rentalFeeItemService", rentalFeeItemService);
    }

    /**
     * 列表：批量回填区县名、最低租金、封面图、标签与配套
     */
    @Test
    @DisplayName("pageApartment：批量回填列表展示字段")
    void pageShouldFillItemFields() {
        AppApartmentPageReqVO reqVO = new AppApartmentPageReqVO();
        RentalApartment po = buildApartment(1L, 3L);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RentalApartment> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setRecords(List.of(po));
        page.setTotal(1L);
        when(rentalApartmentMapper.selectAppApartmentPage(any(), eq(reqVO), eq(null))).thenReturn(page);
        AppApartmentItemRespVO record = new AppApartmentItemRespVO();
        when(rentalAppApartmentConvert.toItemRespVOList(List.of(po))).thenReturn(List.of(record));
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(Map.of(3L, "西湖区"));
        when(rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.APARTMENT, List.of(1L)))
                .thenReturn(Map.of(1L, 66L));
        when(rentalApartmentMapper.selectMinRentByApartmentIds(List.of(1L)))
                .thenReturn(List.of(new ApartmentMinRentBO(1L, new BigDecimal("2500.00"))));
        when(rentalDictService.listDictItemsBatch(any(), any()))
                .thenReturn(Map.of("near_subway", List.of(new DictItemVO("近地铁", "near_subway"))));

        PageRespVO<AppApartmentItemRespVO> result = appApartmentService.pageApartment(reqVO);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(record.getDistrictName()).isEqualTo("西湖区");
        assertThat(record.getPaymentMethodName()).isEqualTo("月付");
        assertThat(record.getMinRent()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(record.getCoverFileId()).isEqualTo(66L);
        assertThat(record.getLabelCodes()).extracting(DictItemVO::getValue).containsExactly("near_subway");
    }

    /**
     * 列表：该市下没有区县时直接返回空页
     */
    @Test
    @DisplayName("pageApartment：市下没有区县时返回空页")
    void pageShouldReturnEmptyWhenCityHasNoDistrict() {
        AppApartmentPageReqVO reqVO = new AppApartmentPageReqVO();
        reqVO.setCityId(2L);
        when(rentalAreaService.listDistrictIdsByCity(2L)).thenReturn(List.of());

        PageRespVO<AppApartmentItemRespVO> result = appApartmentService.pageApartment(reqVO);

        assertThat(result.getTotal()).isZero();
        verify(rentalApartmentMapper, never()).selectAppApartmentPage(any(), any(), any());
    }

    /**
     * 详情：未发布公寓按不存在处理
     */
    @Test
    @DisplayName("getApartment：未发布公寓报公寓不存在")
    void detailShouldRejectUnpublishedApartment() {
        RentalApartment po = buildApartment(1L, 3L);
        po.setPublishStatus(RentalPublishStatusEnum.UNPUBLISHED.getValue());
        when(rentalApartmentMapper.selectById(1L)).thenReturn(po);

        assertThatThrownBy(() -> appApartmentService.getApartment(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.APARTMENT_NOT_FOUND.code()));
    }

    /**
     * 详情：已发布时回填费用项与图片
     */
    @Test
    @DisplayName("getApartment：回填费用项与图片")
    void detailShouldFillFeeItemsAndImages() {
        RentalApartment po = buildApartment(1L, 3L);
        when(rentalApartmentMapper.selectById(1L)).thenReturn(po);
        AppApartmentRespVO respVO = new AppApartmentRespVO();
        when(rentalAppApartmentConvert.toRespVO(po)).thenReturn(respVO);
        when(rentalAreaService.getDistrictNameMap(List.of(3L))).thenReturn(Map.of(3L, "西湖区"));
        when(rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.APARTMENT, List.of(1L)))
                .thenReturn(Map.of());
        when(rentalApartmentMapper.selectMinRentByApartmentIds(List.of(1L))).thenReturn(List.of());
        when(rentalDictService.listDictItemsBatch(any(), any())).thenReturn(Map.of());
        when(rentalFeeItemService.listByApartmentId(1L)).thenReturn(List.of());
        when(rentalImageService.listImages(RentalImageItemTypeEnum.APARTMENT, 1L))
                .thenReturn(List.of(new ImageRespVO()));
        when(rentalAppImageConvert.toAppImageRespVOList(any())).thenReturn(List.of());

        AppApartmentRespVO result = appApartmentService.getApartment(1L);

        assertThat(result.getDistrictName()).isEqualTo("西湖区");
        verify(rentalImageService).listImages(RentalImageItemTypeEnum.APARTMENT, 1L);
        verify(rentalFeeItemService).listByApartmentId(1L);
    }

    /**
     * 构造已发布公寓实体
     *
     * @param id         公寓 ID
     * @param districtId 区县 ID
     * @return 公寓实体
     */
    private RentalApartment buildApartment(Long id, Long districtId) {
        RentalApartment po = new RentalApartment();
        po.setId(id);
        po.setName("文三路公寓");
        po.setDistrictId(districtId);
        po.setPaymentMethod(1);
        po.setPublishStatus(RentalPublishStatusEnum.PUBLISHED.getValue());
        po.setLabelCodes("near_subway");
        po.setFacilityCodes("elevator");
        return po;
    }
}
