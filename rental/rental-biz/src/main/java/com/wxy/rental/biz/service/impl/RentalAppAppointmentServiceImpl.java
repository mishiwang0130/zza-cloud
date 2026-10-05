package com.wxy.rental.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.po.BasePO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.rental.api.dto.ViewAppointmentRespDTO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalAppAppointmentConvert;
import com.wxy.rental.biz.enums.RentalAppointmentStatusEnum;
import com.wxy.rental.biz.enums.RentalPublishStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalViewAppointmentMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalViewAppointment;
import com.wxy.rental.biz.service.RentalAppAppointmentService;
import com.wxy.rental.biz.vo.app.AppViewAppointmentCreateReqVO;
import com.wxy.rental.biz.vo.app.AppViewAppointmentRespVO;
import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户端看房预约实现：提交时校验公寓存在并只记预约人 ID，取消时只允许动自己的待看房预约。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Service
public class RentalAppAppointmentServiceImpl implements RentalAppAppointmentService {

    /** 预约 Mapper */
    @Resource
    private RentalViewAppointmentMapper rentalViewAppointmentMapper;

    /** 公寓 Mapper：校验公寓存在、回填公寓名 */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** App 预约转换器 */
    @Resource
    private RentalAppAppointmentConvert rentalAppAppointmentConvert;

    /**
     * 提交看房预约
     *
     * @param reqVO 预约入参
     * @return 新预约 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAppointment(AppViewAppointmentCreateReqVO reqVO) {
        Long userId = requireLoginUserId();
        RentalApartment apartment = rentalApartmentMapper.selectById(reqVO.getApartmentId());
        if (apartment == null || !RentalPublishStatusEnum.PUBLISHED.getValue().equals(apartment.getPublishStatus())) {
            // 未发布的公寓对 App 视为不存在：用户不该给一个自己在 App 里看不到的公寓留预约，隐藏存在性也避免下架房源被继续传播
            throw new BizException(RentalErrorConstant.APARTMENT_NOT_FOUND);
        }
        RentalViewAppointment po = new RentalViewAppointment();
        po.setUserId(userId);
        po.setApartmentId(reqVO.getApartmentId());
        // 只记预约人 ID：姓名与手机号属于用户档案，后台按 userId 查最新的，不在这张表里快照
        po.setAppointmentTime(reqVO.getAppointmentTime());
        po.setStatus(RentalAppointmentStatusEnum.PENDING.getValue());
        po.setRemark(StringUtils.hasText(reqVO.getRemark()) ? reqVO.getRemark() : "");
        rentalViewAppointmentMapper.insert(po);
        return po.getId();
    }

    /**
     * 分页查询我的看房预约
     *
     * @param reqVO 分页入参
     * @return 分页结果
     */
    @Override
    public PageRespVO<AppViewAppointmentRespVO> pageAppointment(PageReqVO reqVO) {
        Long userId = requireLoginUserId();
        Page<RentalViewAppointment> page = PageUtil.toPage(reqVO);
        IPage<RentalViewAppointment> result = rentalViewAppointmentMapper.selectPage(page,
                new LambdaQueryWrapper<RentalViewAppointment>()
                        .eq(RentalViewAppointment::getUserId, userId)
                        .orderByDesc(RentalViewAppointment::getId));
        List<AppViewAppointmentRespVO> records = rentalAppAppointmentConvert.toRespVOList(result.getRecords());
        if (!records.isEmpty()) {
            records.forEach(record -> record.setStatusName(
                    RentalAppointmentStatusEnum.labelOf(record.getStatus())));
            fillApartmentNames(records);
        }
        return PageUtil.of(result, records);
    }

    /**
     * 取消我的看房预约
     *
     * @param id 预约 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(Long id) {
        Long userId = requireLoginUserId();
        RentalViewAppointment po = id == null ? null : rentalViewAppointmentMapper.selectById(id);
        RentalAppointmentStatusEnum status = po == null ? null : RentalAppointmentStatusEnum.of(po.getStatus());
        if (po == null || !userId.equals(po.getUserId()) || status != RentalAppointmentStatusEnum.PENDING) {
            // 「不是自己的」「已经看过/取消过」都按同一条提示拒绝：不区分原因，避免用别人的预约 ID 试探出状态
            throw new BizException(RentalErrorConstant.APPOINTMENT_CANCEL_FORBIDDEN);
        }
        po.setStatus(RentalAppointmentStatusEnum.CANCELED.getValue());
        rentalViewAppointmentMapper.updateById(po);
    }

    /**
     * 按用户id获取最近的未看房记录
     *
     * @param userId 用户ID
     * @return {@code ViewAppointmentRespDTO }
     * @author wxy
     * @date 2026/10/05
     */
    @Override
    public ViewAppointmentRespDTO getByUserId(Long userId) {
        LambdaQueryWrapper<RentalViewAppointment> rentalViewAppointmentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        rentalViewAppointmentLambdaQueryWrapper.eq(RentalViewAppointment::getUserId,userId);
        rentalViewAppointmentLambdaQueryWrapper.eq(RentalViewAppointment::getStatus,RentalAppointmentStatusEnum.PENDING.getValue());
        rentalViewAppointmentLambdaQueryWrapper.orderByDesc(BasePO::getCreateTime);

        List<RentalViewAppointment> rentalViewAppointments = rentalViewAppointmentMapper.selectList(rentalViewAppointmentLambdaQueryWrapper);
        if (CollUtil.isEmpty(rentalViewAppointments)){
            return null;
        }
        ViewAppointmentRespDTO viewAppointmentRespDTO = new ViewAppointmentRespDTO();
        viewAppointmentRespDTO.setUnViewCount(rentalViewAppointments.size());
        RentalViewAppointment rentalViewAppointment = rentalViewAppointments.get(0);
        viewAppointmentRespDTO.setAppointmentTime(rentalViewAppointment.getAppointmentTime());
        Long apartmentId = rentalViewAppointment.getApartmentId();
        RentalApartment rentalApartment = rentalApartmentMapper.selectById(apartmentId);
        if (rentalApartment != null) {
            String apartmentName = rentalApartment.getName();
            viewAppointmentRespDTO.setApartmentName(apartmentName);
        }
        return viewAppointmentRespDTO;
    }

    /**
     * 取当前登录用户 ID
     *
     * <p>这些接口本身要求登录（没有 {@code @PermitAll}），拿不到 userId 说明请求绕过了登录校验，按未登录处理，而不是写一条没有主人的数据。
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

    /**
     * 批量回填公寓名称
     *
     * @param records 预约返回体列表
     */
    private void fillApartmentNames(List<AppViewAppointmentRespVO> records) {
        List<Long> apartmentIds = records.stream()
                .map(AppViewAppointmentRespVO::getApartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (apartmentIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new LinkedHashMap<>();
        for (RentalApartment apartment : rentalApartmentMapper.selectBatchIds(apartmentIds)) {
            nameMap.put(apartment.getId(), apartment.getName());
        }
        records.forEach(record -> record.setApartmentName(nameMap.get(record.getApartmentId())));
    }
}
