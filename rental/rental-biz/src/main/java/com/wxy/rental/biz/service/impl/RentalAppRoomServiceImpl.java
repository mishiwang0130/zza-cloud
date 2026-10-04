package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.rental.biz.constant.RentalDictTypeConstant;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalAppImageConvert;
import com.wxy.rental.biz.convert.RentalAppRoomConvert;
import com.wxy.rental.biz.enums.RentalAppSortTypeEnum;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.mq.producer.RentalBrowseHistoryProducer;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalAppRoomService;
import com.wxy.rental.biz.service.RentalAreaService;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.service.RentalFeeItemService;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.DictItemVO;
import com.wxy.rental.biz.vo.app.AppRoomItemRespVO;
import com.wxy.rental.biz.vo.app.AppRoomPageReqVO;
import com.wxy.rental.biz.vo.app.AppRoomRespVO;
import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户端房间查询实现：列表批量回填公寓信息与标签，详情额外返回公寓精简信息、图片并补写浏览记录。
 *
 * <p>「记录浏览」放在详情接口里而不是另开写接口：详情页的每一次打开本身就是一次浏览，让前端再调一次写接口既多一次往返，也可能被漏调。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalAppRoomServiceImpl implements RentalAppRoomService {

    /** 房间 Mapper */
    @Resource
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper：房间列表与详情里的公寓字段 */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** App 房间转换器 */
    @Resource
    private RentalAppRoomConvert rentalAppRoomConvert;

    /** App 图片转换器 */
    @Resource
    private RentalAppImageConvert rentalAppImageConvert;

    /** 区划服务：区县名回填、按市展开区县 */
    @Resource
    private RentalAreaService rentalAreaService;

    /** 字典服务：朝向与标签 / 配套中文名 */
    @Resource
    private RentalDictService rentalDictService;

    /** 图片服务：封面图与详情图片 */
    @Resource
    private RentalImageService rentalImageService;

    /** 文件服务：把封面图 fileId 批量换成预签名访问地址 */
    @Resource
    private RentalFileService rentalFileService;

    /** 费用项服务：详情里的公寓费用项 */
    @Resource
    private RentalFeeItemService rentalFeeItemService;

    /** 浏览记录生产者：详情接口异步补写浏览流水 */
    @Resource
    private RentalBrowseHistoryProducer rentalBrowseHistoryProducer;

    /**
     * 分页查询已发布房间
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<AppRoomItemRespVO> pageRoom(AppRoomPageReqVO reqVO) {
        // 关键字去空白、排序值收敛成合法值：非法排序退化成综合排序，空白关键字按不过滤处理
        reqVO.setKeyword(trimToNull(reqVO.getKeyword()));
        reqVO.setSortType(RentalAppSortTypeEnum.normalize(reqVO.getSortType()));
        List<Long> districtIds = reqVO.getCityId() == null
                ? null : rentalAreaService.listDistrictIdsByCity(reqVO.getCityId());
        if (districtIds != null && districtIds.isEmpty()) {
            // 该市下没有区县：直接给空页，避免拼出非法的 IN () 查询
            return PageRespVO.of(0L, reqVO.getPageNum(), reqVO.getPageSize(), List.of());
        }
        Page<RentalRoom> page = PageUtil.toPage(reqVO);
        IPage<RentalRoom> result = rentalRoomMapper.selectAppRoomPage(page, reqVO, districtIds);
        List<RentalRoom> rooms = result.getRecords();
        List<AppRoomItemRespVO> records = rentalAppRoomConvert.toItemRespVOList(rooms);
        if (!rooms.isEmpty()) {
            fillItemFields(records, rooms);
        }
        return PageUtil.of(result, records);
    }

    /**
     * 查询已发布房间详情，并异步补写浏览记录
     *
     * @param id 房间 ID
     * @return 房间详情
     */
    @Override
    @Transactional(readOnly = true)
    public AppRoomRespVO getRoom(Long id) {
        RentalRoom room = id == null ? null : rentalRoomMapper.selectById(id);
        if (room == null || !RentalPublishStatusEnum.PUBLISHED.getValue().equals(room.getPublishStatus())) {
            // 未发布房间对 App 视为不存在：下架（没有删除接口）之后就不该再被访问到
            throw new BizException(RentalErrorConstant.ROOM_NOT_FOUND);
        }
        RentalApartment apartment = rentalApartmentMapper.selectById(room.getApartmentId());
        if (apartment == null || !RentalPublishStatusEnum.PUBLISHED.getValue().equals(apartment.getPublishStatus())) {
            // 公寓下架后它名下的房间也不再对外展示，否则用户能通过旧链接看到「已下架公寓里的房间」
            throw new BizException(RentalErrorConstant.ROOM_NOT_FOUND);
        }
        AppRoomRespVO respVO = rentalAppRoomConvert.toRespVO(room);
        fillItemFields(List.of(respVO), List.of(room), Map.of(apartment.getId(), apartment));
        respVO.setAddressDetail(apartment.getAddressDetail());
        respVO.setPhone(apartment.getPhone());
        respVO.setIntroduction(apartment.getIntroduction());
        respVO.setFeeItems(rentalFeeItemService.listByApartmentId(apartment.getId()));
        respVO.setImages(rentalAppImageConvert.toAppImageRespVOList(
                rentalImageService.listImages(RentalImageItemTypeEnum.ROOM, room.getId())));
        publishBrowseHistory(room.getId());
        return respVO;
    }

    /**
     * 异步补写浏览记录
     *
     * <p>未登录（拿不到 userId）时不记录；发送失败由生产者吞成日志，不影响详情返回。
     *
     * @param roomId 被浏览的房间 ID
     */
    private void publishBrowseHistory(Long roomId) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return;
        }
        rentalBrowseHistoryProducer.send(userId, roomId);
    }

    /**
     * 回填列表 / 详情的跨表展示字段（公寓信息在本方法内批量查）
     *
     * @param records 返回体列表（顺序与实体列表一一对应）
     * @param rooms   房间实体列表
     */
    private void fillItemFields(List<? extends AppRoomItemRespVO> records, List<RentalRoom> rooms) {
        fillItemFields(records, rooms, loadApartments(rooms));
    }

    /**
     * 回填列表 / 详情的跨表展示字段
     *
     * @param records      返回体列表（顺序与实体列表一一对应）
     * @param rooms        房间实体列表
     * @param apartmentMap 房间所属公寓，键为公寓 ID
     */
    private void fillItemFields(List<? extends AppRoomItemRespVO> records, List<RentalRoom> rooms,
                                Map<Long, RentalApartment> apartmentMap) {
        List<Long> roomIds = rooms.stream().map(RentalRoom::getId).filter(Objects::nonNull).toList();
        Map<Long, Long> coverFileIdMap = rentalImageService.listCoverFileIdMap(
                RentalImageItemTypeEnum.ROOM, roomIds);
        // 一次把本页所有封面图换成预签名地址（coverFileIdMap 为空时内部不再调 infra）
        Map<Long, String> coverFileUrlMap = rentalFileService.getFileUrlMap(coverFileIdMap.values());
        Map<Long, String> districtNameMap = rentalAreaService.getDistrictNameMap(
                apartmentMap.values().stream().map(RentalApartment::getDistrictId).toList());
        Map<String, String> orientationMap = rentalDictService.getLabelMap(RentalDictTypeConstant.ROOM_ORIENTATION);
        Map<String, List<DictItemVO>> labelMap = rentalDictService.listDictItemsBatch(
                RentalDictTypeConstant.ROOM_LABEL, rooms.stream().map(RentalRoom::getLabelCodes).toList());
        Map<String, List<DictItemVO>> facilityMap = rentalDictService.listDictItemsBatch(
                RentalDictTypeConstant.ROOM_FACILITY, rooms.stream().map(RentalRoom::getFacilityCodes).toList());
        for (int index = 0; index < records.size(); index++) {
            AppRoomItemRespVO record = records.get(index);
            RentalRoom room = rooms.get(index);
            RentalApartment apartment = apartmentMap.get(room.getApartmentId());
            record.setApartmentName(apartment == null ? null : apartment.getName());
            record.setDistrictId(apartment == null ? null : apartment.getDistrictId());
            record.setDistrictName(apartment == null ? null : districtNameMap.get(apartment.getDistrictId()));
            record.setDepositMonths(apartment == null ? null : apartment.getDepositMonths());
            record.setPaymentMethod(apartment == null ? null : apartment.getPaymentMethod());
            record.setMinLeaseMonths(apartment == null ? null : apartment.getMinLeaseMonths());
            record.setOrientationName(orientationMap.get(room.getOrientation()));
            Long coverFileId = coverFileIdMap.get(room.getId());
            record.setCoverFileId(coverFileId);
            record.setCoverFileUrl(coverFileId == null ? null : coverFileUrlMap.get(coverFileId));
            record.setLabelCodes(labelMap.getOrDefault(room.getLabelCodes(), List.of()));
            record.setFacilityCodes(facilityMap.getOrDefault(room.getFacilityCodes(), List.of()));
        }
    }

    /**
     * 关键字去首尾空格，空白串统一收敛为 null
     *
     * @param keyword 原关键字，可以为 null
     * @return 去空白后的关键字；原值为空白时返回 null
     */
    private String trimToNull(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 批量查房间所属公寓
     *
     * @param rooms 房间实体列表
     * @return 公寓 ID 到公寓实体的映射
     */
    private Map<Long, RentalApartment> loadApartments(List<RentalRoom> rooms) {
        List<Long> apartmentIds = rooms.stream()
                .map(RentalRoom::getApartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (apartmentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RentalApartment> apartmentMap = new LinkedHashMap<>();
        for (RentalApartment apartment : rentalApartmentMapper.selectBatchIds(apartmentIds)) {
            apartmentMap.put(apartment.getId(), apartment);
        }
        return apartmentMap;
    }
}
