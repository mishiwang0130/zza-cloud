package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalAppImageConvert;
import com.wxy.rental.biz.convert.RentalAppLeaseConvert;
import com.wxy.rental.biz.enums.RentalImageItemTypeEnum;
import com.wxy.rental.biz.enums.RentalLeaseStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalLeaseMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalLease;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalAppLeaseService;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.service.RentalImageService;
import com.wxy.rental.biz.vo.app.AppLeaseItemRespVO;
import com.wxy.rental.biz.vo.app.AppLeaseRespVO;
import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户端租约实现：列表 / 详情按需回填，三个动作固定状态迁移。
 *
 * <p>每次动作前先做归属校验，再做状态校验：顺序反了会把「别人的租约处于什么状态」这样的信息通过错误码泄露出去。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalAppLeaseServiceImpl implements RentalAppLeaseService {

    /** 租约 Mapper */
    @Resource
    private RentalLeaseMapper rentalLeaseMapper;

    /** 房间 Mapper：回填房间号与图片 */
    @Resource
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper：回填公寓名 */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** 图片服务：封面图与详情图片 */
    @Resource
    private RentalImageService rentalImageService;

    /** 文件服务：合同文件换预签名地址 */
    @Resource
    private RentalFileService rentalFileService;

    /** App 图片转换器 */
    @Resource
    private RentalAppImageConvert rentalAppImageConvert;

    /** App 租约转换器 */
    @Resource
    private RentalAppLeaseConvert rentalAppLeaseConvert;

    /**
     * 分页查询我的租约
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    @Override
    public PageRespVO<AppLeaseItemRespVO> pageLease(PageReqVO reqVO) {
        Long userId = requireLoginUserId();
        Page<RentalLease> page = PageUtil.toPage(reqVO);
        IPage<RentalLease> result = rentalLeaseMapper.selectPage(page, new LambdaQueryWrapper<RentalLease>()
                .eq(RentalLease::getUserId, userId)
                .orderByDesc(RentalLease::getId));
        List<RentalLease> leases = result.getRecords();
        List<AppLeaseItemRespVO> records = rentalAppLeaseConvert.toItemRespVOList(leases);
        if (!leases.isEmpty()) {
            fillItemFields(records, leases);
        }
        return PageUtil.of(result, records);
    }

    /**
     * 查询我的租约详情（含合同文件地址与房间图片）
     *
     * @param id 租约 ID
     * @return 租约详情
     */
    @Override
    @Transactional(readOnly = true)
    public AppLeaseRespVO getLease(Long id) {
        RentalLease po = getMyLease(id);
        AppLeaseRespVO respVO = rentalAppLeaseConvert.toRespVO(po);
        fillItemFields(List.of(respVO), List.of(po));
        respVO.setContractFileUrl(contractFileUrl(po.getContractFileId()));
        respVO.setImages(rentalAppImageConvert.toAppImageRespVOList(
                rentalImageService.listImages(RentalImageItemTypeEnum.ROOM, po.getRoomId())));
        return respVO;
    }

    /**
     * 确认签约：1 签约待确认 → 2 已签约
     *
     * @param id 租约 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        changeStatus(id, RentalLeaseStatusEnum.PENDING_CONFIRM, RentalLeaseStatusEnum.SIGNED);
    }

    /**
     * 申请退租：2 已签约 → 5 退租待确认
     *
     * @param id 租约 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyWithdraw(Long id) {
        changeStatus(id, RentalLeaseStatusEnum.SIGNED, RentalLeaseStatusEnum.WITHDRAW_PENDING);
    }

    /**
     * 申请续约：2 已签约 → 7 续约待确认
     *
     * @param id 租约 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRenew(Long id) {
        changeStatus(id, RentalLeaseStatusEnum.SIGNED, RentalLeaseStatusEnum.RENEW_PENDING);
    }

    /**
     * 按「必须是本人 + 必须从指定状态出发」流转租约状态
     *
     * @param id             租约 ID
     * @param expectedStatus 允许的起始状态
     * @param targetStatus   目标状态
     */
    private void changeStatus(Long id, RentalLeaseStatusEnum expectedStatus, RentalLeaseStatusEnum targetStatus) {
        RentalLease po = getMyLease(id);
        RentalLeaseStatusEnum currentStatus = RentalLeaseStatusEnum.of(po.getStatus());
        if (currentStatus != expectedStatus || !currentStatus.canTransitionTo(targetStatus)) {
            throw new BizException(RentalErrorConstant.LEASE_STATUS_TRANSITION_INVALID);
        }
        po.setStatus(targetStatus.getValue());
        rentalLeaseMapper.updateById(po);
    }

    /**
     * 取当前用户的租约，别人的租约按不存在处理
     *
     * @param id 租约 ID
     * @return 租约实体
     */
    private RentalLease getMyLease(Long id) {
        Long userId = requireLoginUserId();
        RentalLease po = id == null ? null : rentalLeaseMapper.selectById(id);
        if (po == null || !userId.equals(po.getUserId())) {
            // 别人的租约按不存在处理：不回「无权访问」，否则可以用 ID 试探出别人租约的存在性
            throw new BizException(RentalErrorConstant.LEASE_NOT_FOUND);
        }
        return po;
    }

    /**
     * 取合同文件的预签名地址
     *
     * @param contractFileId 合同文件 ID，0 表示尚未上传
     * @return 预签名地址；未上传或文件查不到时返回 null
     */
    private String contractFileUrl(Long contractFileId) {
        if (contractFileId == null || contractFileId == 0L) {
            return null;
        }
        return rentalFileService.getFileUrlMap(List.of(contractFileId)).get(contractFileId);
    }

    /**
     * 回填列表 / 详情的跨表展示字段
     *
     * @param records 返回体列表（顺序与实体列表一一对应）
     * @param leases  租约实体列表
     */
    private void fillItemFields(List<? extends AppLeaseItemRespVO> records, List<RentalLease> leases) {
        List<Long> roomIds = leases.stream().map(RentalLease::getRoomId).filter(Objects::nonNull).distinct().toList();
        Map<Long, RentalRoom> roomMap = loadRooms(roomIds);
        Map<Long, RentalApartment> apartmentMap = loadApartments(
                roomMap.values().stream().map(RentalRoom::getApartmentId).toList());
        Map<Long, Long> coverFileIdMap = rentalImageService.listCoverFileIdMap(RentalImageItemTypeEnum.ROOM, roomIds);
        for (int index = 0; index < records.size(); index++) {
            AppLeaseItemRespVO record = records.get(index);
            RentalLease lease = leases.get(index);
            RentalRoom room = roomMap.get(lease.getRoomId());
            RentalApartment apartment = apartmentMap.get(lease.getApartmentId());
            record.setApartmentName(apartment == null ? null : apartment.getName());
            record.setRoomNumber(room == null ? null : room.getRoomNumber());
            record.setStatusName(RentalLeaseStatusEnum.labelOf(lease.getStatus()));
            record.setCoverFileId(coverFileIdMap.get(lease.getRoomId()));
        }
    }

    /**
     * 批量查房间
     *
     * @param roomIds 房间 ID 列表
     * @return 房间 ID 到房间实体的映射
     */
    private Map<Long, RentalRoom> loadRooms(List<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RentalRoom> roomMap = new LinkedHashMap<>();
        for (RentalRoom room : rentalRoomMapper.selectBatchIds(roomIds)) {
            roomMap.put(room.getId(), room);
        }
        return roomMap;
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

    /**
     * 取当前登录用户 ID
     *
     * <p>租约接口都要求登录（没有 {@code @PermitAll}），拿不到 userId 说明请求绕过了登录校验，按未登录处理。
     *
     * @return 登录用户 ID
     */
    private Long requireLoginUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }
}
