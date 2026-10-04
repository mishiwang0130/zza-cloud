package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalFeeItemConvert;
import com.wxy.rental.biz.mapper.RentalApartmentFeeMapper;
import com.wxy.rental.biz.mapper.RentalFeeItemMapper;
import com.wxy.rental.biz.po.RentalFeeItem;
import com.wxy.rental.biz.vo.admin.FeeItemCreateReqVO;
import com.wxy.rental.biz.vo.admin.FeeItemRespVO;
import com.wxy.rental.biz.vo.admin.FeeItemUpdateReqVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 费用项服务单元测试：名称唯一、被引用不能删、单位缺省处理。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalFeeItemServiceImplTest {

    /** 费用项 Mapper */
    @Mock
    private RentalFeeItemMapper rentalFeeItemMapper;

    /** 公寓费用项关联 Mapper */
    @Mock
    private RentalApartmentFeeMapper rentalApartmentFeeMapper;

    /** 费用项转换器 */
    @Mock
    private RentalFeeItemConvert rentalFeeItemConvert;

    /** 被测服务 */
    private RentalFeeItemServiceImpl feeItemService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        feeItemService = new RentalFeeItemServiceImpl();
        ReflectionTestUtils.setField(feeItemService, "rentalFeeItemMapper", rentalFeeItemMapper);
        ReflectionTestUtils.setField(feeItemService, "rentalApartmentFeeMapper", rentalApartmentFeeMapper);
        ReflectionTestUtils.setField(feeItemService, "rentalFeeItemConvert", rentalFeeItemConvert);
    }

    /**
     * 新增：名称重复时报「费用项名称已存在」
     */
    @Test
    @DisplayName("createFeeItem：名称重复时拒绝")
    void createShouldRejectDuplicateName() {
        when(rentalFeeItemMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> feeItemService.createFeeItem(buildCreateReq("水费", "吨")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.FEE_ITEM_NAME_EXISTS.code()));
        verify(rentalFeeItemMapper, never()).insert(any(RentalFeeItem.class));
    }

    /**
     * 新增：单位不传时落空串，避免库里出现 null 与空串两种「没填」
     */
    @Test
    @DisplayName("createFeeItem：单位不传时落空串")
    void createShouldDefaultUnit() {
        when(rentalFeeItemMapper.selectCount(any())).thenReturn(0L);

        feeItemService.createFeeItem(buildCreateReq("宽带费", null));

        ArgumentCaptor<RentalFeeItem> captor = ArgumentCaptor.forClass(RentalFeeItem.class);
        verify(rentalFeeItemMapper).insert(captor.capture());
        assertThat(captor.getValue().getUnit()).isEmpty();
        assertThat(captor.getValue().getName()).isEqualTo("宽带费");
    }

    /**
     * 修改：费用项不存在时报「费用项不存在」
     */
    @Test
    @DisplayName("updateFeeItem：费用项不存在时报错")
    void updateShouldRejectMissingFeeItem() {
        when(rentalFeeItemMapper.selectById(9L)).thenReturn(null);
        FeeItemUpdateReqVO reqVO = new FeeItemUpdateReqVO();
        reqVO.setId(9L);
        reqVO.setName("水费");
        reqVO.setAmount(BigDecimal.TEN);

        assertThatThrownBy(() -> feeItemService.updateFeeItem(reqVO))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.FEE_ITEM_NOT_FOUND.code()));
    }

    /**
     * 删除：被公寓引用时拒绝
     */
    @Test
    @DisplayName("deleteFeeItem：被公寓引用时拒绝删除")
    void deleteShouldRejectReferencedFeeItem() {
        when(rentalFeeItemMapper.selectById(1L)).thenReturn(new RentalFeeItem());
        when(rentalApartmentFeeMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> feeItemService.deleteFeeItem(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.FEE_ITEM_IN_USE.code()));
        verify(rentalFeeItemMapper, never()).deleteById(1L);
    }

    /**
     * 删除：没有被引用时逻辑删除
     */
    @Test
    @DisplayName("deleteFeeItem：未被引用时逻辑删除")
    void deleteShouldRemoveUnreferencedFeeItem() {
        when(rentalFeeItemMapper.selectById(1L)).thenReturn(new RentalFeeItem());
        when(rentalApartmentFeeMapper.selectCount(any())).thenReturn(0L);

        feeItemService.deleteFeeItem(1L);

        verify(rentalFeeItemMapper).deleteById(1L);
    }

    /**
     * 列表：按创建顺序返回，交给转换器转返回体
     */
    @Test
    @DisplayName("listFeeItem：按创建顺序返回")
    void listShouldReturnConvertedList() {
        RentalFeeItem po = new RentalFeeItem();
        po.setName("水费");
        when(rentalFeeItemMapper.selectList(any())).thenReturn(List.of(po));
        FeeItemRespVO respVO = new FeeItemRespVO();
        respVO.setName("水费");
        when(rentalFeeItemConvert.toRespVOList(List.of(po))).thenReturn(List.of(respVO));

        List<FeeItemRespVO> list = feeItemService.listFeeItem();

        assertThat(list).extracting(FeeItemRespVO::getName).containsExactly("水费");
    }

    /**
     * 构造新增入参
     *
     * @param name 名称
     * @param unit 单位
     * @return 新增入参
     */
    private FeeItemCreateReqVO buildCreateReq(String name, String unit) {
        FeeItemCreateReqVO reqVO = new FeeItemCreateReqVO();
        reqVO.setName(name);
        reqVO.setAmount(new BigDecimal("12.50"));
        reqVO.setUnit(unit);
        return reqVO;
    }
}
