package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.po.BasePO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.infra.api.dto.AppUserSimpleDTO;
import com.wxy.rental.api.dto.LeaseRespDTO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalLeaseConvert;
import com.wxy.rental.biz.enums.RentalLeaseSourceTypeEnum;
import com.wxy.rental.biz.enums.RentalLeaseStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalLeaseMapper;
import com.wxy.rental.biz.mapper.RentalRoomMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalLease;
import com.wxy.rental.biz.po.RentalRoom;
import com.wxy.rental.biz.service.RentalAppUserService;
import com.wxy.rental.biz.service.RentalLeaseService;
import com.wxy.rental.biz.vo.admin.LeaseCreateReqVO;
import com.wxy.rental.biz.vo.admin.LeasePageItemRespVO;
import com.wxy.rental.biz.vo.admin.LeasePageReqVO;
import com.wxy.rental.biz.vo.admin.LeaseRespVO;
import com.wxy.rental.biz.vo.admin.LeaseUpdateReqVO;
import com.wxy.rental.biz.vo.admin.LeaseUpdateStatusReqVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.events.Event;

/**
 * 租约服务实现：签约校验、条款维护与状态机。
 *
 * <p>签约时的三条硬约束：房间必须存在且属于提交的公寓、结束日期必须晚于开始日期、房间不能已有
 * 生效中的租约（状态 1/2/5）。押金不传时按「租金 × 公寓押金月数」计算，避免运营手算出错。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalLeaseServiceImpl implements RentalLeaseService {

    /** 金额保留两位小数，避免 租金 × 月数 出现 2500.000000 这类展示噪音 */
    private static final int AMOUNT_SCALE = 2;

    /** 租约 Mapper */
    @Resource
    private RentalLeaseMapper rentalLeaseMapper;

    /** 房间 Mapper：校验房间与所属公寓、回填房间号 */
    @Resource
    private RentalRoomMapper rentalRoomMapper;

    /** 公寓 Mapper：取押金月数、回填公寓名称 */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** 租约转换器 */
    @Resource
    private RentalLeaseConvert rentalLeaseConvert;

    /** 用户档案服务：按 userId 回填承租人昵称与手机号 */
    @Resource
    private RentalAppUserService rentalAppUserService;


    /**
     * 新增租约
     *
     * @param reqVO 新增入参
     * @return 新租约 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLease(LeaseCreateReqVO reqVO) {
        RentalRoom room = getExistingRoom(reqVO.getRoomId());
        if (!room.getApartmentId().equals(reqVO.getApartmentId())) {
            throw new BizException(RentalErrorConstant.ROOM_APARTMENT_MISMATCH);
        }
        validateDateRange(reqVO.getLeaseStartDate(), reqVO.getLeaseEndDate());
        if (rentalRoomMapper.countEffectiveLeases(room.getId()) > 0) {
            throw new BizException(RentalErrorConstant.ROOM_LEASE_EXISTS);
        }
        RentalApartment apartment = getExistingApartment(reqVO.getApartmentId());
        RentalLease po = new RentalLease();
        po.setUserId(reqVO.getUserId());
        po.setApartmentId(reqVO.getApartmentId());
        po.setRoomId(reqVO.getRoomId());
        po.setContractFileId(reqVO.getContractFileId() == null ? 0L : reqVO.getContractFileId());
        po.setLeaseStartDate(reqVO.getLeaseStartDate());
        po.setLeaseEndDate(reqVO.getLeaseEndDate());
        po.setRent(reqVO.getRent());
        po.setDeposit(reqVO.getDeposit() == null
                ? calcDeposit(reqVO.getRent(), apartment.getDepositMonths()) : reqVO.getDeposit());
        // 新建租约一律「签约待确认」：合同先落库，等确认签约后再变成生效中的租约
        po.setStatus(RentalLeaseStatusEnum.PENDING_CONFIRM.getValue());
        po.setSourceType(reqVO.getSourceType() == null
                ? RentalLeaseSourceTypeEnum.NEW.getValue() : reqVO.getSourceType());
        po.setRemark(defaultString(reqVO.getRemark()));
        rentalLeaseMapper.insert(po);
        return po.getId();
    }

    /**
     * 修改租约条款
     *
     * @param reqVO 修改入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLease(LeaseUpdateReqVO reqVO) {
        RentalLease po = getExistingLease(reqVO.getId());
        LocalDate effectiveStartDate = reqVO.getLeaseStartDate() == null
                ? po.getLeaseStartDate() : reqVO.getLeaseStartDate();
        LocalDate effectiveEndDate = reqVO.getLeaseEndDate() == null
                ? po.getLeaseEndDate() : reqVO.getLeaseEndDate();
        validateDateRange(effectiveStartDate, effectiveEndDate);
        RentalLeaseStatusEnum currentStatus = RentalLeaseStatusEnum.of(po.getStatus());
        if (currentStatus != null && currentStatus.isFinal()) {
            // 终态租约只允许补合同文件与改备注：提交原值不算改动，便于前端整表提交
            assertTermUnchanged("租约开始日期", po.getLeaseStartDate(), reqVO.getLeaseStartDate());
            assertTermUnchanged("租约结束日期", po.getLeaseEndDate(), reqVO.getLeaseEndDate());
            assertTermUnchanged("签约月租金", po.getRent(), reqVO.getRent());
            assertTermUnchanged("押金", po.getDeposit(), reqVO.getDeposit());
        }
        if (reqVO.getContractFileId() != null) {
            po.setContractFileId(reqVO.getContractFileId());
        }
        if (reqVO.getLeaseStartDate() != null) {
            po.setLeaseStartDate(reqVO.getLeaseStartDate());
        }
        if (reqVO.getLeaseEndDate() != null) {
            po.setLeaseEndDate(reqVO.getLeaseEndDate());
        }
        if (reqVO.getRent() != null) {
            po.setRent(reqVO.getRent());
        }
        if (reqVO.getDeposit() != null) {
            po.setDeposit(reqVO.getDeposit());
        }
        if (reqVO.getRemark() != null) {
            po.setRemark(reqVO.getRemark());
        }
        rentalLeaseMapper.updateById(po);
    }

    /**
     * 查询租约详情
     *
     * @param id 租约 ID
     * @return 租约详情
     */
    @Override
    @Transactional(readOnly = true)
    public LeaseRespVO getLease(Long id) {
        RentalLease po = getExistingLease(id);
        LeaseRespVO respVO = rentalLeaseConvert.toRespVO(po);
        RentalApartment apartment = rentalApartmentMapper.selectById(po.getApartmentId());
        respVO.setApartmentName(apartment == null ? null : apartment.getName());
        RentalRoom room = rentalRoomMapper.selectById(po.getRoomId());
        respVO.setRoomNumber(room == null ? null : room.getRoomNumber());
        respVO.setStatusName(RentalLeaseStatusEnum.labelOf(po.getStatus()));
        // 承租人昵称与手机按 userId 查用户档案：租约只存 ID，用户改了资料这里拿到的就是最新值
        AppUserSimpleDTO user = getAppUser(po.getUserId());
        if (user != null) {
            respVO.setUserNickname(user.getNickname());
            respVO.setUserMobile(user.getMobile());
        }
        return respVO;
    }

    /**
     * 分页查询租约列表
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<LeasePageItemRespVO> pageLease(LeasePageReqVO reqVO) {
        Page<LeasePageItemRespVO> page = PageUtil.toPage(reqVO);
        IPage<LeasePageItemRespVO> result = rentalLeaseMapper.selectLeasePage(page, reqVO);
        List<LeasePageItemRespVO> records = result.getRecords();
        records.forEach(record -> record.setStatusName(RentalLeaseStatusEnum.labelOf(record.getStatus())));
        fillAppUsers(records);
        return PageUtil.of(result, records);
    }

    /**
     * 租约状态流转
     *
     * @param reqVO 状态流转入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(LeaseUpdateStatusReqVO reqVO) {
        RentalLease po = getExistingLease(reqVO.getId());
        RentalLeaseStatusEnum currentStatus = RentalLeaseStatusEnum.of(po.getStatus());
        RentalLeaseStatusEnum targetStatus = RentalLeaseStatusEnum.of(reqVO.getStatus());
        if (currentStatus == null || !currentStatus.canTransitionTo(targetStatus)) {
            throw new BizException(RentalErrorConstant.LEASE_STATUS_TRANSITION_INVALID);
        }
        po.setStatus(targetStatus.getValue());
        rentalLeaseMapper.updateById(po);
    }

    @Override
    public LeaseRespDTO getLeaseInfoByUserId(Long userId) {
        LambdaQueryWrapper<RentalLease> rentalLeaseLambdaQueryWrapper = new LambdaQueryWrapper<>();
        rentalLeaseLambdaQueryWrapper.eq(RentalLease::getUserId,userId);
        rentalLeaseLambdaQueryWrapper.orderByDesc(BasePO::getCreateTime);
        rentalLeaseLambdaQueryWrapper.last("limit 1");
        RentalLease rentalLease = rentalLeaseMapper.selectOne(rentalLeaseLambdaQueryWrapper);
        LeaseRespDTO respDTO = rentalLeaseConvert.toRespDTO(rentalLease);

        respDTO.setApartmentName(getExistingApartment(rentalLease.getApartmentId()).getName());

        respDTO.setRoomNumber(getExistingRoom(rentalLease.getRoomId()).getRoomNumber());
        return respDTO;
    }

    /**
     * 按 ID 查租约，查不到直接报错
     *
     * @param id 租约 ID
     * @return 租约实体
     */
    private RentalLease getExistingLease(Long id) {
        RentalLease po = id == null ? null : rentalLeaseMapper.selectById(id);
        if (po == null) {
            throw new BizException(RentalErrorConstant.LEASE_NOT_FOUND);
        }
        return po;
    }

    /**
     * 按 ID 查房间，查不到直接报错
     *
     * @param roomId 房间 ID
     * @return 房间实体
     */
    private RentalRoom getExistingRoom(Long roomId) {
        RentalRoom room = roomId == null ? null : rentalRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(RentalErrorConstant.ROOM_NOT_FOUND);
        }
        return room;
    }

    /**
     * 按 ID 查公寓，查不到直接报错
     *
     * @param apartmentId 公寓 ID
     * @return 公寓实体
     */
    private RentalApartment getExistingApartment(Long apartmentId) {
        RentalApartment apartment = apartmentId == null ? null : rentalApartmentMapper.selectById(apartmentId);
        if (apartment == null) {
            throw new BizException(RentalErrorConstant.APARTMENT_NOT_FOUND);
        }
        return apartment;
    }

    /**
     * 校验租约日期区间
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new BizException(RentalErrorConstant.LEASE_DATE_INVALID);
        }
    }

    /**
     * 校验条款没有真的被改动（终态租约用）
     *
     * @param fieldName 字段名，用于错误提示
     * @param current   库里的值
     * @param submitted 本次提交的值，为 null 表示没提交
     */
    private void assertTermUnchanged(String fieldName, Object current, Object submitted) {
        if (submitted != null && !Objects.equals(current, submitted)) {
            throw new BizException(RentalErrorConstant.LEASE_UPDATE_FORBIDDEN,
                    "该状态的租约不允许修改" + fieldName);
        }
    }

    /**
     * 按「租金 × 押金月数」计算押金
     *
     * @param rent          月租金
     * @param depositMonths 公寓押金月数，可以为 null（按 0 处理）
     * @return 押金，保留两位小数
     */
    private BigDecimal calcDeposit(BigDecimal rent, Integer depositMonths) {
        int months = depositMonths == null ? 0 : depositMonths;
        return rent.multiply(BigDecimal.valueOf(months)).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
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

    /**
     * 查单个承租人档案
     *
     * @param userId 承租人 App 用户 ID，可以为 null
     * @return 用户档案，userId 为空或查不到时返回 null
     */
    private AppUserSimpleDTO getAppUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return rentalAppUserService.getAppUserMap(List.of(userId)).get(userId);
    }

    /**
     * 批量回填承租人昵称与手机号
     *
     * <p>租约表只存 {@code userId}，这里收集本页用户 ID 一次批量查 infra，避免逐行回源；
     * 用户已删除或查不到时保持 null，不给整页翻页制造失败。
     *
     * @param records 租约列表返回体
     */
    private void fillAppUsers(List<LeasePageItemRespVO> records) {
        List<Long> userIds = records.stream()
                .map(LeasePageItemRespVO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, AppUserSimpleDTO> userMap = rentalAppUserService.getAppUserMap(userIds);
        records.forEach(record -> {
            AppUserSimpleDTO user = userMap.get(record.getUserId());
            if (user != null) {
                record.setUserNickname(user.getNickname());
                record.setUserMobile(user.getMobile());
            }
        });
    }
}
