package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wxy.common.core.exception.BizException;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalFeeItemConvert;
import com.wxy.rental.biz.mapper.RentalApartmentFeeMapper;
import com.wxy.rental.biz.mapper.RentalFeeItemMapper;
import com.wxy.rental.biz.po.RentalApartmentFee;
import com.wxy.rental.biz.po.RentalFeeItem;
import com.wxy.rental.biz.service.RentalFeeItemService;
import com.wxy.rental.biz.vo.admin.FeeItemCreateReqVO;
import com.wxy.rental.biz.vo.admin.FeeItemRespVO;
import com.wxy.rental.biz.vo.admin.FeeItemUpdateReqVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 费用项服务实现：名称唯一与「被引用不能删」两条规则。
 *
 * <p>名称唯一按「未删除的数据」判断，不依赖库级唯一索引：库上的唯一索引会把逻辑删除的行也算进去，
 * 「删了同名再建」就会报 Duplicate entry，而费用项删除后重建是正常操作。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalFeeItemServiceImpl implements RentalFeeItemService {

    /** 费用项 Mapper */
    @Resource
    private RentalFeeItemMapper rentalFeeItemMapper;

    /** 公寓费用项关联 Mapper：删除前判断是否被引用 */
    @Resource
    private RentalApartmentFeeMapper rentalApartmentFeeMapper;

    /** 费用项转换器 */
    @Resource
    private RentalFeeItemConvert rentalFeeItemConvert;

    /**
     * 新增费用项
     *
     * @param reqVO 新增入参
     * @return 新费用项 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFeeItem(FeeItemCreateReqVO reqVO) {
        if (countByName(reqVO.getName(), null) > 0) {
            throw new BizException(RentalErrorConstant.FEE_ITEM_NAME_EXISTS);
        }
        RentalFeeItem po = new RentalFeeItem();
        po.setName(reqVO.getName());
        po.setAmount(reqVO.getAmount());
        po.setUnit(defaultString(reqVO.getUnit()));
        rentalFeeItemMapper.insert(po);
        return po.getId();
    }

    /**
     * 修改费用项
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeeItem(FeeItemUpdateReqVO reqVO) {
        RentalFeeItem po = getExistingFeeItem(reqVO.getId());
        if (countByName(reqVO.getName(), po.getId()) > 0) {
            throw new BizException(RentalErrorConstant.FEE_ITEM_NAME_EXISTS);
        }
        po.setName(reqVO.getName());
        po.setAmount(reqVO.getAmount());
        if (reqVO.getUnit() != null) {
            // 单位可以为空串（表示无单位），所以只在「没传」时不动它
            po.setUnit(reqVO.getUnit());
        }
        rentalFeeItemMapper.updateById(po);
    }

    /**
     * 删除费用项（逻辑删除）
     *
     * @param id 费用项 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFeeItem(Long id) {
        getExistingFeeItem(id);
        Long referencedCount = rentalApartmentFeeMapper.selectCount(new LambdaQueryWrapper<RentalApartmentFee>()
                .eq(RentalApartmentFee::getFeeItemId, id));
        if (referencedCount != null && referencedCount > 0) {
            throw new BizException(RentalErrorConstant.FEE_ITEM_IN_USE);
        }
        rentalFeeItemMapper.deleteById(id);
    }

    /**
     * 查询全部费用项，按创建顺序返回
     *
     * @return 费用项列表
     */
    @Override
    public List<FeeItemRespVO> listFeeItem() {
        List<RentalFeeItem> feeItems = rentalFeeItemMapper.selectList(new LambdaQueryWrapper<RentalFeeItem>()
                .orderByAsc(RentalFeeItem::getId));
        return rentalFeeItemConvert.toRespVOList(feeItems);
    }

    /**
     * 按 ID 查费用项，查不到直接报错
     *
     * @param id 费用项 ID
     * @return 费用项实体
     */
    private RentalFeeItem getExistingFeeItem(Long id) {
        RentalFeeItem po = id == null ? null : rentalFeeItemMapper.selectById(id);
        if (po == null) {
            throw new BizException(RentalErrorConstant.FEE_ITEM_NOT_FOUND);
        }
        return po;
    }

    /**
     * 统计同名费用项数量，排除自身
     *
     * @param name      费用项名称
     * @param excludeId 需要排除的费用项 ID，新增时传 null
     * @return 数量
     */
    private long countByName(String name, Long excludeId) {
        Long count = rentalFeeItemMapper.selectCount(new LambdaQueryWrapper<RentalFeeItem>()
                .eq(RentalFeeItem::getName, name)
                .ne(excludeId != null, RentalFeeItem::getId, excludeId));
        return count == null ? 0L : count;
    }

    /**
     * 把可空字符串收敛为空串，避免库里出现 null 与空串两种「没填」
     *
     * @param value 原值，可以为 null
     * @return 原值或空串
     */
    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
