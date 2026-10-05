package com.wxy.rental.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.util.RemoteCallUtil;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.po.BasePO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.infra.api.client.InfraAreaClient;
import com.wxy.infra.api.dto.AreaDTO;
import com.wxy.rental.api.dto.RoomSearchReqDTO;
import com.wxy.rental.api.dto.RoomSummaryDTO;
import com.wxy.rental.biz.constant.RentalDictTypeConstant;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalRoomConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.service.RentalRoomService;
import com.wxy.rental.biz.vo.admin.RoomCreateReqVO;
import com.wxy.rental.biz.vo.admin.RoomPageItemRespVO;
import com.wxy.rental.biz.vo.admin.RoomPageReqVO;
import com.wxy.rental.biz.vo.admin.RoomRespVO;
import com.wxy.rental.biz.vo.admin.RoomSimpleRespVO;
import com.wxy.rental.biz.vo.admin.RoomUpdatePublishStatusReqVO;
import com.wxy.rental.biz.vo.admin.RoomUpdateReqVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 房间服务实现：房间号在公寓内唯一、入住状态由租约派生。
 *
 * <p>「房间是否在租」不做落库字段：它随租约变化，落库就得靠定时任务或事件同步，容易不一致；
 * 列表与下架校验都按租约的生效状态（1/2/5）现算。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalRoomServiceImpl implements RentalRoomService {

    /** 房间 Mapper */
    @Resource
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper：校验所属公寓存在、回填公寓名称 */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** 房间转换器 */
    @Resource
    private RentalRoomConvert rentalRoomConvert;

    /** 字典服务：标签 / 配套 / 朝向的中文名 */
    @Resource
    private RentalDictService rentalDictService;

    /** 图片服务：图片覆盖写与详情回填 */
    @Resource
    private RentalImageService rentalImageService;

    @Resource
    private InfraAreaClient infraAreaClient;

    /**
     * 新增房间：主表与图片在同一个事务里落库
     *
     * @param reqVO 新增入参
     * @return 新房间 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoom(RoomCreateReqVO reqVO) {
        validateApartmentExists(reqVO.getApartmentId());
        if (countByRoomNumber(reqVO.getApartmentId(), reqVO.getRoomNumber(), null) > 0) {
            throw new BizException(RentalErrorConstant.ROOM_NUMBER_EXISTS);
        }
        RentalRoom po = new RentalRoom();
        po.setApartmentId(reqVO.getApartmentId());
        po.setRoomNumber(reqVO.getRoomNumber());
        po.setRent(reqVO.getRent());
        po.setArea(reqVO.getArea() == null ? BigDecimal.ZERO : reqVO.getArea());
        po.setRoomCount(reqVO.getRoomCount() == null ? 1 : reqVO.getRoomCount());
        po.setOrientation(defaultString(reqVO.getOrientation()));
        po.setFloorNo(defaultString(reqVO.getFloorNo()));
        // 新建房间一律未发布：先落数据再决定对外展示，避免刚建一半就被 App 端看到
        po.setPublishStatus(RentalPublishStatusEnum.UNPUBLISHED.getValue());
        po.setLabelCodes(rentalDictService.joinCodes(RentalDictTypeConstant.ROOM_LABEL, reqVO.getLabelCodes()));
        po.setFacilityCodes(rentalDictService.joinCodes(RentalDictTypeConstant.ROOM_FACILITY,
                reqVO.getFacilityCodes()));
        validateOrientation(reqVO.getOrientation());
        rentalRoomMapper.insert(po);
        rentalImageService.replaceImages(RentalImageItemTypeEnum.ROOM, po.getId(), reqVO.getImages());
        return po.getId();
    }

    /**
     * 修改房间：入参为 null 的字段表示不改动，图片按提交的列表覆盖写
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoom(RoomUpdateReqVO reqVO) {
        RentalRoom po = getExistingRoom(reqVO.getId());
        validateApartmentExists(reqVO.getApartmentId());
        if (countByRoomNumber(reqVO.getApartmentId(), reqVO.getRoomNumber(), po.getId()) > 0) {
            throw new BizException(RentalErrorConstant.ROOM_NUMBER_EXISTS);
        }
        validateOrientation(reqVO.getOrientation());
        po.setApartmentId(reqVO.getApartmentId());
        po.setRoomNumber(reqVO.getRoomNumber());
        po.setRent(reqVO.getRent());
        po.setArea(reqVO.getArea());
        po.setRoomCount(reqVO.getRoomCount());
        po.setOrientation(reqVO.getOrientation());
        po.setFloorNo(reqVO.getFloorNo());
        if (reqVO.getLabelCodes() != null) {
            po.setLabelCodes(rentalDictService.joinCodes(RentalDictTypeConstant.ROOM_LABEL, reqVO.getLabelCodes()));
        }
        if (reqVO.getFacilityCodes() != null) {
            po.setFacilityCodes(rentalDictService.joinCodes(RentalDictTypeConstant.ROOM_FACILITY,
                    reqVO.getFacilityCodes()));
        }
        rentalRoomMapper.updateById(po);
        rentalImageService.replaceImages(RentalImageItemTypeEnum.ROOM, po.getId(), reqVO.getImages());
    }

    /**
     * 查询房间详情（含公寓名称、标签与配套中文名、图片）
     *
     * @param id 房间 ID
     * @return 房间详情
     */
    @Override
    @Transactional(readOnly = true)
    public RoomRespVO getRoom(Long id) {
        RentalRoom po = getExistingRoom(id);
        RoomRespVO respVO = rentalRoomConvert.toRespVO(po);
        RentalApartment apartment = rentalApartmentMapper.selectById(po.getApartmentId());
        respVO.setApartmentName(apartment == null ? null : apartment.getName());
        respVO.setLabelCodes(rentalDictService.listDictItems(RentalDictTypeConstant.ROOM_LABEL,
                po.getLabelCodes()));
        respVO.setFacilityCodes(rentalDictService.listDictItems(RentalDictTypeConstant.ROOM_FACILITY,
                po.getFacilityCodes()));
        respVO.setImages(rentalImageService.listImages(RentalImageItemTypeEnum.ROOM, po.getId()));
        return respVO;
    }

    /**
     * 分页查询房间列表（含公寓名称与入住状态）
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<RoomPageItemRespVO> pageRoom(RoomPageReqVO reqVO) {
        Page<RoomPageItemRespVO> page = PageUtil.toPage(reqVO);
        IPage<RoomPageItemRespVO> result = rentalRoomMapper.selectRoomPage(page, reqVO);
        List<RoomPageItemRespVO> records = result.getRecords();
        if (!records.isEmpty()) {
            // 一次拿到朝向字典，再逐行回填，避免每行都去访问一次缓存
            Map<String, String> orientationMap = rentalDictService.getLabelMap(RentalDictTypeConstant.ROOM_ORIENTATION);
            records.forEach(record -> record.setOrientationName(orientationMap.get(record.getOrientation())));
        }
        return PageUtil.of(result, records);
    }

    /**
     * 房间上架 / 下架
     *
     * @param reqVO 上架 / 下架入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePublishStatus(RoomUpdatePublishStatusReqVO reqVO) {
        RentalRoom po = getExistingRoom(reqVO.getId());
        if (RentalPublishStatusEnum.UNPUBLISHED.getValue().equals(reqVO.getPublishStatus())
                && rentalRoomMapper.countEffectiveLeases(po.getId()) > 0) {
            // 房间下架后 App 端不再展示它，但生效中的租约还在，出现「客户已入住却查不到房间」的矛盾状态
            throw new BizException(RentalErrorConstant.ROOM_HAS_LEASE);
        }
        po.setPublishStatus(reqVO.getPublishStatus());
        rentalRoomMapper.updateById(po);
    }

    /**
     * 查询某个公寓下的房间精简信息（租约选房下拉用）
     *
     * @param apartmentId 公寓 ID
     * @return 房间精简列表
     */
    @Override
    public List<RoomSimpleRespVO> listSimpleByApartment(Long apartmentId) {
        if (apartmentId == null) {
            return List.of();
        }
        List<RentalRoom> rooms = rentalRoomMapper.selectList(new LambdaQueryWrapper<RentalRoom>()
                .eq(RentalRoom::getApartmentId, apartmentId)
                .orderByAsc(RentalRoom::getRoomNumber));
        return rentalRoomConvert.toSimpleRespVOList(rooms);
    }

    @Override
    public List<RoomSummaryDTO> searchAvailableRooms(RoomSearchReqDTO roomSearchReqDTO) {
        String cityName = roomSearchReqDTO.getCityName();
        List<Long> areaIdList = new ArrayList<>();
        List<Long> apartmentIdList = new ArrayList<>();
        if (StrUtil.isNotBlank(cityName)){
            // 远程调用infra去获取该城市的区县对象列表
            List<AreaDTO> areaDTOList = RemoteCallUtil.call(
                    () -> infraAreaClient.listByCityName(cityName).requireData(), "查询城市下的区县");
            if (CollUtil.isNotEmpty(areaDTOList)) {
                // 从列表中取出区县id列表
                areaIdList = areaDTOList.stream().map(AreaDTO::getId).toList();
            }
        }
        // 根据区县id列表查询公寓列表
        LambdaQueryWrapper<RentalApartment> wrapper = Wrappers.<RentalApartment>lambdaQuery()
                .in(RentalApartment::getDistrictId, areaIdList)
                .like(StrUtil.isNotBlank(roomSearchReqDTO.getApartmentName()), RentalApartment::getName, roomSearchReqDTO.getApartmentName());
        List<RentalApartment> rentalApartmentList = rentalApartmentMapper.selectList(wrapper);
        Map<Long, String> apartmentMap = rentalApartmentList.stream().collect(Collectors.toMap(BasePO::getId, RentalApartment::getName, (k1, k2) -> k1));
        // 取出公寓id列表作为房间表的查询条件
        apartmentIdList = rentalApartmentList.stream().map(BasePO::getId).toList();

        LambdaQueryWrapper<RentalRoom> rentalRoomLambdaQueryWrapper = new LambdaQueryWrapper<>();
        rentalRoomLambdaQueryWrapper.in(true,RentalRoom::getApartmentId,apartmentIdList);
        rentalRoomLambdaQueryWrapper.ge(roomSearchReqDTO.getMinRent()!= null,RentalRoom::getRent,roomSearchReqDTO.getMinRent());
        rentalRoomLambdaQueryWrapper.le(roomSearchReqDTO.getMaxRent()!=null,RentalRoom::getRent,roomSearchReqDTO.getMaxRent());
        rentalRoomLambdaQueryWrapper.eq(roomSearchReqDTO.getRoomCount()!= null,RentalRoom::getRoomCount,roomSearchReqDTO.getRoomCount());
        List<RentalRoom> rentalRooms = rentalRoomMapper.selectList(rentalRoomLambdaQueryWrapper);
        return rentalRooms.stream().map(item -> {
            RoomSummaryDTO roomSummaryDTO = new RoomSummaryDTO();
            roomSummaryDTO.setApartmentName(apartmentMap.get(item.getApartmentId()));
            roomSummaryDTO.setRoomNumber(item.getRoomNumber());
            roomSummaryDTO.setRent(item.getRent());
            return roomSummaryDTO;
        }).toList();
    }

    /**
     * 按 ID 查房间，查不到直接报错
     *
     * @param id 房间 ID
     * @return 房间实体
     */
    private RentalRoom getExistingRoom(Long id) {
        RentalRoom po = id == null ? null : rentalRoomMapper.selectById(id);
        if (po == null) {
            throw new BizException(RentalErrorConstant.ROOM_NOT_FOUND);
        }
        return po;
    }

    /**
     * 校验所属公寓存在
     *
     * @param apartmentId 公寓 ID
     */
    private void validateApartmentExists(Long apartmentId) {
        RentalApartment apartment = apartmentId == null ? null : rentalApartmentMapper.selectById(apartmentId);
        if (apartment == null) {
            throw new BizException(RentalErrorConstant.APARTMENT_NOT_FOUND);
        }
    }

    /**
     * 校验朝向编码存在于字典
     *
     * @param orientation 朝向编码，可以为空（表示不填朝向）
     */
    private void validateOrientation(String orientation) {
        if (!StringUtils.hasText(orientation)) {
            return;
        }
        // 复用字典的写入校验：朝向不在字典里就报 DICT_CODE_INVALID，避免存进一个查不到中文名的编码
        rentalDictService.joinCodes(RentalDictTypeConstant.ROOM_ORIENTATION, List.of(orientation));
    }

    /**
     * 统计同一公寓下同名房间号数量，排除自身
     *
     * @param apartmentId 公寓 ID
     * @param roomNumber  房间号
     * @param excludeId   需要排除的房间 ID，新增时传 null
     * @return 数量
     */
    private long countByRoomNumber(Long apartmentId, String roomNumber, Long excludeId) {
        Long count = rentalRoomMapper.selectCount(new LambdaQueryWrapper<RentalRoom>()
                .eq(RentalRoom::getApartmentId, apartmentId)
                .eq(RentalRoom::getRoomNumber, roomNumber)
                .ne(excludeId != null, RentalRoom::getId, excludeId));
        return count == null ? 0L : count;
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
