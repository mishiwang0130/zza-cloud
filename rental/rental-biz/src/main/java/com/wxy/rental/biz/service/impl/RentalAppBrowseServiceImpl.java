package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.rental.biz.convert.RentalAppBrowseConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalBrowseHistoryMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalBrowseHistory;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalAppBrowseService;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.app.AppRoomBrowseRespVO;
import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 用户端浏览记录实现：分页取自己的流水，再批量回填房间号、公寓名、租金与封面图。
 *
 * <p>只查自己：浏览记录是个人数据，不能按房间或按时间跨用户查。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalAppBrowseServiceImpl implements RentalAppBrowseService {

    /** 浏览记录 Mapper */
    @Resource
    private RentalBrowseHistoryMapper rentalBrowseHistoryMapper;

    /** 房间 Mapper：回填房间号与租金 */
    @Resource
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper：回填公寓名 */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** 图片服务：回填封面图 */
    @Resource
    private RentalImageService rentalImageService;

    /** 文件服务：把封面图 fileId 批量换成预签名访问地址 */
    @Resource
    private RentalFileService rentalFileService;

    /** App 浏览记录转换器 */
    @Resource
    private RentalAppBrowseConvert rentalAppBrowseConvert;

    /**
     * 分页查询我的浏览记录，按浏览时间倒序
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    @Override
    public PageRespVO<AppRoomBrowseRespVO> pageBrowse(PageReqVO reqVO) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        Page<RentalBrowseHistory> page = PageUtil.toPage(reqVO);
        // 浏览时间就是 create_time：按它倒序就是「最近看的排最前」，同一时间点的记录再用主键保序
        IPage<RentalBrowseHistory> result = rentalBrowseHistoryMapper.selectPage(page,
                new LambdaQueryWrapper<RentalBrowseHistory>()
                        .eq(RentalBrowseHistory::getUserId, userId)
                        .orderByDesc(RentalBrowseHistory::getCreateTime)
                        .orderByDesc(RentalBrowseHistory::getId));
        List<AppRoomBrowseRespVO> records = rentalAppBrowseConvert.toRespVOList(result.getRecords());
        if (!records.isEmpty()) {
            fillRoomFields(records);
        }
        return PageUtil.of(result, records);
    }

    /**
     * 批量回填房间号、公寓名、租金与封面图
     *
     * @param records 浏览记录返回体列表
     */
    private void fillRoomFields(List<AppRoomBrowseRespVO> records) {
        List<Long> roomIds = records.stream()
                .map(AppRoomBrowseRespVO::getRoomId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (roomIds.isEmpty()) {
            return;
        }
        Map<Long, RentalRoom> roomMap = new LinkedHashMap<>();
        for (RentalRoom room : rentalRoomMapper.selectBatchIds(roomIds)) {
            roomMap.put(room.getId(), room);
        }
        List<Long> apartmentIds = roomMap.values().stream().map(RentalRoom::getApartmentId).toList();
        Map<Long, RentalApartment> apartmentMap = loadApartments(apartmentIds);
        Map<Long, Long> coverFileIdMap = rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.ROOM, roomIds);
        // 一次把本页所有封面图换成预签名地址（coverFileIdMap 为空时内部不再调 infra）
        Map<Long, String> coverFileUrlMap = rentalFileService.getFileUrlMap(coverFileIdMap.values());
        for (AppRoomBrowseRespVO record : records) {
            RentalRoom room = roomMap.get(record.getRoomId());
            if (room == null) {
                // 房间被物理删除或记录早于房间迁移：保留流水本身，只把回填字段留空，不让整页失败
                continue;
            }
            RentalApartment apartment = apartmentMap.get(room.getApartmentId());
            record.setRoomNumber(room.getRoomNumber());
            record.setRent(room.getRent());
            record.setApartmentId(room.getApartmentId());
            record.setApartmentName(apartment == null ? null : apartment.getName());
            Long coverFileId = coverFileIdMap.get(room.getId());
            record.setCoverFileId(coverFileId);
            record.setCoverFileUrl(coverFileId == null ? null : coverFileUrlMap.get(coverFileId));
        }
    }

    /**
     * 批量查公寓
     *
     * @param apartmentIds 公寓 ID 列表（可能含 null、重复）
     * @return 公寓 ID 到公寓实体的映射
     */
    private Map<Long, RentalApartment> loadApartments(List<Long> apartmentIds) {
        List<Long> distinctIds = apartmentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RentalApartment> apartmentMap = new LinkedHashMap<>();
        for (RentalApartment apartment : rentalApartmentMapper.selectBatchIds(distinctIds)) {
            apartmentMap.put(apartment.getId(), apartment);
        }
        return apartmentMap;
    }
}
