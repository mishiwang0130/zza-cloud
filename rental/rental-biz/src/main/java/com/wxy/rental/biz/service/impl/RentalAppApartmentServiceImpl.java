package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.rental.biz.bo.ApartmentMinRentBO;
import com.wxy.rental.biz.constant.RentalDictTypeConstant;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalAppApartmentConvert;
import com.wxy.rental.biz.convert.RentalAppImageConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalPaymentMethodEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.service.RentalAppApartmentService;
import com.wxy.rental.biz.service.RentalAreaService;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.service.RentalFeeItemService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.DictItemVO;
import com.wxy.rental.biz.vo.app.AppApartmentItemRespVO;
import com.wxy.rental.biz.vo.app.AppApartmentPageReqVO;
import com.wxy.rental.biz.vo.app.AppApartmentRespVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户端公寓查询实现：SQL 负责「哪些公寓该出现」，Service 负责把展示字段补齐。
 *
 * <p>列表页的跨表字段一次批量取：区县名、封面图、最低租金、标签 / 配套中文名各一次查询 / 一次字典加载，不逐行回查。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalAppApartmentServiceImpl implements RentalAppApartmentService {

    /** 公寓 Mapper */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** App 公寓转换器 */
    @Resource
    private RentalAppApartmentConvert rentalAppApartmentConvert;

    /** App 图片转换器 */
    @Resource
    private RentalAppImageConvert rentalAppImageConvert;

    /** 区划服务：区县名回填、按市展开区县 */
    @Resource
    private RentalAreaService rentalAreaService;

    /** 字典服务：标签 / 配套中文名 */
    @Resource
    private RentalDictService rentalDictService;

    /** 图片服务：封面图与详情图片 */
    @Resource
    private RentalImageService rentalImageService;

    /** 费用项服务：详情里的费用项 */
    @Resource
    private RentalFeeItemService rentalFeeItemService;

    /**
     * 分页查询已发布公寓
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<AppApartmentItemRespVO> pageApartment(AppApartmentPageReqVO reqVO) {
        List<Long> districtIds = reqVO.getCityId() == null
                ? null : rentalAreaService.listDistrictIdsByCity(reqVO.getCityId());
        if (districtIds != null && districtIds.isEmpty()) {
            // 该市下没有区县：直接给空页，避免拼出非法的 IN () 查询
            return PageRespVO.of(0L, reqVO.getPageNum(), reqVO.getPageSize(), List.of());
        }
        Page<RentalApartment> page = PageUtil.toPage(reqVO);
        IPage<RentalApartment> result = rentalApartmentMapper.selectAppApartmentPage(page, reqVO, districtIds);
        List<RentalApartment> apartments = result.getRecords();
        List<AppApartmentItemRespVO> records = rentalAppApartmentConvert.toItemRespVOList(apartments);
        if (!apartments.isEmpty()) {
            fillItemFields(records, apartments);
        }
        return PageUtil.of(result, records);
    }

    /**
     * 查询已发布公寓详情
     *
     * @param id 公寓 ID
     * @return 公寓详情
     */
    @Override
    @Transactional(readOnly = true)
    public AppApartmentRespVO getApartment(Long id) {
        RentalApartment po = id == null ? null : rentalApartmentMapper.selectById(id);
        if (po == null || !RentalPublishStatusEnum.PUBLISHED.getValue().equals(po.getPublishStatus())) {
            // 未发布的公寓对 App 视为不存在：下架（没有删除接口）之后就不该再被访问到，也不该暴露它的存在
            throw new BizException(RentalErrorConstant.APARTMENT_NOT_FOUND);
        }
        AppApartmentRespVO respVO = rentalAppApartmentConvert.toRespVO(po);
        fillItemFields(List.of(respVO), List.of(po));
        respVO.setFeeItems(rentalFeeItemService.listByApartmentId(po.getId()));
        respVO.setImages(rentalAppImageConvert.toAppImageRespVOList(
                rentalImageService.listImages(RentalImageItemTypeEnum.APARTMENT, po.getId())));
        return respVO;
    }

    /**
     * 回填列表 / 详情的跨表展示字段
     *
     * @param records    返回体列表（顺序与实体列表一一对应）
     * @param apartments 公寓实体列表
     */
    private void fillItemFields(List<? extends AppApartmentItemRespVO> records, List<RentalApartment> apartments) {
        List<Long> apartmentIds = apartments.stream().map(RentalApartment::getId).filter(Objects::nonNull).toList();
        Map<Long, String> districtNameMap = rentalAreaService.getDistrictNameMap(
                apartments.stream().map(RentalApartment::getDistrictId).toList());
        Map<Long, Long> coverFileIdMap = rentalImageService.listCoverFileIdMap(
                RentalImageItemTypeEnum.APARTMENT, apartmentIds);
        Map<Long, BigDecimal> minRentMap = minRentMap(apartmentIds);
        Map<String, List<DictItemVO>> labelMap = rentalDictService.listDictItemsBatch(
                RentalDictTypeConstant.APARTMENT_LABEL,
                apartments.stream().map(RentalApartment::getLabelCodes).toList());
        Map<String, List<DictItemVO>> facilityMap = rentalDictService.listDictItemsBatch(
                RentalDictTypeConstant.APARTMENT_FACILITY,
                apartments.stream().map(RentalApartment::getFacilityCodes).toList());
        for (int index = 0; index < records.size(); index++) {
            AppApartmentItemRespVO record = records.get(index);
            RentalApartment po = apartments.get(index);
            record.setDistrictName(districtNameMap.get(po.getDistrictId()));
            record.setPaymentMethodName(RentalPaymentMethodEnum.labelOf(po.getPaymentMethod()));
            record.setMinRent(minRentMap.get(po.getId()));
            record.setCoverFileId(coverFileIdMap.get(po.getId()));
            record.setLabelCodes(labelMap.getOrDefault(po.getLabelCodes(), List.of()));
            record.setFacilityCodes(facilityMap.getOrDefault(po.getFacilityCodes(), List.of()));
        }
    }

    /**
     * 批量取公寓最低租金
     *
     * @param apartmentIds 公寓 ID 列表
     * @return 公寓 ID 到最低租金的映射；没有已发布房间的公寓不出现在映射里
     */
    private Map<Long, BigDecimal> minRentMap(List<Long> apartmentIds) {
        if (apartmentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> minRentMap = new LinkedHashMap<>();
        for (ApartmentMinRentBO minRent : rentalApartmentMapper.selectMinRentByApartmentIds(apartmentIds)) {
            minRentMap.put(minRent.getApartmentId(), minRent.getMinRent());
        }
        return minRentMap;
    }
}
