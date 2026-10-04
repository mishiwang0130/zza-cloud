package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.rental.biz.constant.RentalDictTypeConstant;
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
import com.wxy.rental.biz.service.RentalApartmentService;
import com.wxy.rental.biz.service.RentalAreaService;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.admin.ApartmentCreateReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageItemRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentPageReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentSimpleRespVO;
import com.wxy.rental.biz.vo.admin.ApartmentUpdatePublishStatusReqVO;
import com.wxy.rental.biz.vo.admin.ApartmentUpdateReqVO;
import com.wxy.rental.biz.vo.admin.FeeItemSimpleRespVO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公寓服务实现：表单一次落全、查询按需组装。
 *
 * <p>查询侧的分工：主表字段一次查出来，区县名、标签 / 配套中文名、费用项、图片分别按需组装；
 * 列表只额外算两个聚合数字（房间总数、空置房间数），不把房间明细带出来。
 *
 * <p>写入侧的分工：主表与两张关联表在同一个事务里；费用项关联与图片覆盖写前都物理删除
 * （逻辑删除会占着唯一键 / 越查越多），提交的 ID 先校验真实存在。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalApartmentServiceImpl implements RentalApartmentService {

    /** 公寓 Mapper */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** 公寓费用项关联 Mapper */
    @Resource
    private RentalApartmentFeeMapper rentalApartmentFeeMapper;

    /** 费用项 Mapper：校验提交的费用项 ID 真实存在 */
    @Resource
    private RentalFeeItemMapper rentalFeeItemMapper;

    /** 公寓转换器 */
    @Resource
    private RentalApartmentConvert rentalApartmentConvert;

    /** 费用项转换器 */
    @Resource
    private RentalFeeItemConvert rentalFeeItemConvert;

    /** 字典服务：标签 / 配套编解码与中文名回填 */
    @Resource
    private RentalDictService rentalDictService;

    /** 区划服务：区县名回填与按市展开区县 */
    @Resource
    private RentalAreaService rentalAreaService;

    /** 图片服务：图片覆盖写与详情回填 */
    @Resource
    private RentalImageService rentalImageService;

    /**
     * 新增公寓：主表、费用项关联与图片在同一个事务里落库
     *
     * @param reqVO 新增入参
     * @return 新公寓 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApartment(ApartmentCreateReqVO reqVO) {
        validateDistrictExists(reqVO.getDistrictId());
        RentalApartment po = new RentalApartment();
        po.setName(reqVO.getName());
        po.setIntroduction(defaultString(reqVO.getIntroduction()));
        po.setDistrictId(reqVO.getDistrictId());
        po.setAddressDetail(defaultString(reqVO.getAddressDetail()));
        po.setPhone(defaultString(reqVO.getPhone()));
        // 起租月数、押金月数与付款方式留空时按最小可用值兜底：公寓必须先能签出一份租约
        po.setMinLeaseMonths(reqVO.getMinLeaseMonths() == null ? 1 : reqVO.getMinLeaseMonths());
        po.setDepositMonths(reqVO.getDepositMonths() == null ? 1 : reqVO.getDepositMonths());
        po.setPaymentMethod(reqVO.getPaymentMethod() == null
                ? RentalPaymentMethodEnum.MONTHLY.getValue() : reqVO.getPaymentMethod());
        // 新建公寓一律未发布：先落数据再决定对外展示，避免刚建一半就被 App 端看到
        po.setPublishStatus(RentalPublishStatusEnum.UNPUBLISHED.getValue());
        po.setLabelCodes(rentalDictService.joinCodes(RentalDictTypeConstant.APARTMENT_LABEL, reqVO.getLabelCodes()));
        po.setFacilityCodes(rentalDictService.joinCodes(RentalDictTypeConstant.APARTMENT_FACILITY,
                reqVO.getFacilityCodes()));
        rentalApartmentMapper.insert(po);
        replaceApartmentFees(po.getId(), reqVO.getFeeItemIds());
        rentalImageService.replaceImages(RentalImageItemTypeEnum.APARTMENT, po.getId(), reqVO.getImages());
        return po.getId();
    }

    /**
     * 修改公寓：入参为 null 的字段表示不改动，费用项与图片按提交的列表覆盖写
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApartment(ApartmentUpdateReqVO reqVO) {
        RentalApartment po = getExistingApartment(reqVO.getId());
        validateDistrictExists(reqVO.getDistrictId());
        po.setName(reqVO.getName());
        po.setIntroduction(reqVO.getIntroduction());
        po.setDistrictId(reqVO.getDistrictId());
        po.setAddressDetail(reqVO.getAddressDetail());
        po.setPhone(reqVO.getPhone());
        po.setMinLeaseMonths(reqVO.getMinLeaseMonths());
        po.setDepositMonths(reqVO.getDepositMonths());
        po.setPaymentMethod(reqVO.getPaymentMethod());
        if (reqVO.getLabelCodes() != null) {
            po.setLabelCodes(rentalDictService.joinCodes(RentalDictTypeConstant.APARTMENT_LABEL,
                    reqVO.getLabelCodes()));
        }
        if (reqVO.getFacilityCodes() != null) {
            po.setFacilityCodes(rentalDictService.joinCodes(RentalDictTypeConstant.APARTMENT_FACILITY,
                    reqVO.getFacilityCodes()));
        }
        rentalApartmentMapper.updateById(po);
        replaceApartmentFees(po.getId(), reqVO.getFeeItemIds());
        rentalImageService.replaceImages(RentalImageItemTypeEnum.APARTMENT, po.getId(), reqVO.getImages());
    }

    /**
     * 查询公寓详情（含区县名、标签 / 配套中文名、费用项与图片）
     *
     * @param id 公寓 ID
     * @return 公寓详情
     */
    @Override
    @Transactional(readOnly = true)
    public ApartmentRespVO getApartment(Long id) {
        RentalApartment po = getExistingApartment(id);
        ApartmentRespVO respVO = rentalApartmentConvert.toRespVO(po);
        respVO.setDistrictName(rentalAreaService.getDistrictNameMap(List.of(po.getDistrictId()))
                .get(po.getDistrictId()));
        respVO.setPaymentMethodName(RentalPaymentMethodEnum.labelOf(po.getPaymentMethod()));
        respVO.setLabelCodes(rentalDictService.listDictItems(RentalDictTypeConstant.APARTMENT_LABEL,
                po.getLabelCodes()));
        respVO.setFacilityCodes(rentalDictService.listDictItems(RentalDictTypeConstant.APARTMENT_FACILITY,
                po.getFacilityCodes()));
        respVO.setFeeItems(listApartmentFees(po.getId()));
        respVO.setImages(rentalImageService.listImages(RentalImageItemTypeEnum.APARTMENT, po.getId()));
        return respVO;
    }

    /**
     * 分页查询公寓列表（含房间总数与空置房间数）
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<ApartmentPageItemRespVO> pageApartment(ApartmentPageReqVO reqVO) {
        List<Long> districtIds = null;
        if (reqVO.getCityId() != null) {
            // 公寓存的是区县 ID，按市筛选要先用 infra 的区划接口把市展开成区县列表
            districtIds = rentalAreaService.listDistrictIdsByCity(reqVO.getCityId());
            if (districtIds.isEmpty()) {
                // 该市下没有区县：直接给空页，避免拼出非法的 IN () 查询
                return PageRespVO.of(0L, reqVO.getPageNum(), reqVO.getPageSize(), List.of());
            }
        }
        Page<ApartmentPageItemRespVO> page = PageUtil.toPage(reqVO);
        IPage<ApartmentPageItemRespVO> result = rentalApartmentMapper.selectApartmentPage(page, reqVO, districtIds);
        List<ApartmentPageItemRespVO> records = result.getRecords();
        Map<Long, String> districtNameMap = rentalAreaService.getDistrictNameMap(
                records.stream().map(ApartmentPageItemRespVO::getDistrictId).toList());
        records.forEach(record -> record.setDistrictName(districtNameMap.get(record.getDistrictId())));
        return PageUtil.of(result, records);
    }

    /**
     * 公寓上架 / 下架
     *
     * @param reqVO 上架 / 下架入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePublishStatus(ApartmentUpdatePublishStatusReqVO reqVO) {
        RentalApartment po = getExistingApartment(reqVO.getId());
        if (RentalPublishStatusEnum.UNPUBLISHED.getValue().equals(reqVO.getPublishStatus())
                && rentalApartmentMapper.countPublishedRooms(po.getId()) > 0) {
            // 公寓下架后 App 端列表不再展示它，但已发布的房间还能被搜到，出现「有房无公寓」的矛盾状态
            throw new BizException(RentalErrorConstant.APARTMENT_HAS_ROOM);
        }
        po.setPublishStatus(reqVO.getPublishStatus());
        rentalApartmentMapper.updateById(po);
    }

    /**
     * 查询全部公寓的精简信息（房间表单的公寓下拉用）
     *
     * @return 公寓精简列表
     */
    @Override
    public List<ApartmentSimpleRespVO> listSimple() {
        List<RentalApartment> apartments = rentalApartmentMapper.selectList(new LambdaQueryWrapper<RentalApartment>()
                .orderByDesc(RentalApartment::getId));
        return rentalApartmentConvert.toSimpleRespVOList(apartments);
    }

    /**
     * 覆盖写公寓的费用项关联
     *
     * @param apartmentId 公寓 ID
     * @param feeItemIds  费用项 ID 列表；为 null 表示本次不改动
     */
    private void replaceApartmentFees(Long apartmentId, List<Long> feeItemIds) {
        if (feeItemIds == null) {
            return;
        }
        List<Long> distinctIds = distinct(feeItemIds);
        if (!distinctIds.isEmpty()) {
            List<RentalFeeItem> feeItems = rentalFeeItemMapper.selectBatchIds(distinctIds);
            if (feeItems.size() != distinctIds.size()) {
                throw new BizException(RentalErrorConstant.FEE_ITEM_NOT_FOUND);
            }
        }
        // 关联表覆盖写必须物理删除：逻辑删除会让旧行继续占着 (apartment_id, fee_item_id) 唯一键
        rentalApartmentFeeMapper.deleteByApartmentId(apartmentId);
        for (Long feeItemId : distinctIds) {
            RentalApartmentFee relation = new RentalApartmentFee();
            relation.setApartmentId(apartmentId);
            relation.setFeeItemId(feeItemId);
            rentalApartmentFeeMapper.insert(relation);
        }
    }

    /**
     * 查询公寓包含的费用项
     *
     * @param apartmentId 公寓 ID
     * @return 费用项精简列表，没有关联时返回空列表
     */
    private List<FeeItemSimpleRespVO> listApartmentFees(Long apartmentId) {
        List<RentalApartmentFee> relations = rentalApartmentFeeMapper.selectList(
                new LambdaQueryWrapper<RentalApartmentFee>()
                        .eq(RentalApartmentFee::getApartmentId, apartmentId)
                        .orderByAsc(RentalApartmentFee::getId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> feeItemIds = relations.stream().map(RentalApartmentFee::getFeeItemId).toList();
        return rentalFeeItemConvert.toSimpleRespVOList(rentalFeeItemMapper.selectBatchIds(feeItemIds));
    }

    /**
     * 按 ID 查公寓，查不到直接报错
     *
     * @param id 公寓 ID
     * @return 公寓实体
     */
    private RentalApartment getExistingApartment(Long id) {
        RentalApartment po = id == null ? null : rentalApartmentMapper.selectById(id);
        if (po == null) {
            throw new BizException(RentalErrorConstant.APARTMENT_NOT_FOUND);
        }
        return po;
    }

    /**
     * 校验区县存在
     *
     * @param districtId 区县 ID
     */
    private void validateDistrictExists(Long districtId) {
        String districtName = districtId == null ? null
                : rentalAreaService.getDistrictNameMap(List.of(districtId)).get(districtId);
        if (districtName == null) {
            throw new BizException(RentalErrorConstant.AREA_NOT_FOUND);
        }
    }

    /**
     * 去重并过滤 null，避免把重复的费用项 ID 写进关联表
     *
     * @param ids 原始 ID 列表
     * @return 去重后的 ID 列表
     */
    private List<Long> distinct(List<Long> ids) {
        return new ArrayList<>(new LinkedHashSet<>(ids.stream().filter(Objects::nonNull).toList()));
    }

    /**
     * 把可空字符串收敛为空串，避免库里出现 null 与空串两种「没填」
     *
     * @param value 原值，可以为 null
     * @return 原值或空串
     */
    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
