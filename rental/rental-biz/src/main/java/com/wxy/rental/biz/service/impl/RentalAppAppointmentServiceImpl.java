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
import com.wxy.rental.biz.convert.RentalAppAppointmentConvert;
import com.wxy.rental.biz.enums.RentalAppointmentStatusEnum;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户端看房预约实现：提交时校验公寓存在并快照联系方式，取消时只允许动自己的待看房预约。
 *
 * @author wxy
 * @date 2026/10/04
 */
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
        if (apartment == null) {
            throw new BizException(RentalErrorConstant.APARTMENT_NOT_FOUND);
        }
        RentalViewAppointment po = new RentalViewAppointment();
        po.setUserId(userId);
        po.setApartmentId(reqVO.getApartmentId());
        // 姓名与手机号按提交值快照：之后用户改了资料，这条预约仍要能按当时的联系方式找到人
        po.setName(reqVO.getName());
        po.setMobile(reqVO.getMobile());
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
