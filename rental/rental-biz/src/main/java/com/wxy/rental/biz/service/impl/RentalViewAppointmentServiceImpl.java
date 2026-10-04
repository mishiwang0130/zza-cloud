package com.wxy.rental.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.convert.RentalViewAppointmentConvert;
import com.wxy.rental.biz.enums.RentalAppointmentStatusEnum;
import com.wxy.rental.biz.mapper.RentalApartmentMapper;
import com.wxy.rental.biz.mapper.RentalViewAppointmentMapper;
import com.wxy.rental.biz.po.RentalApartment;
import com.wxy.rental.biz.po.RentalViewAppointment;
import com.wxy.rental.biz.service.RentalViewAppointmentService;
import com.wxy.rental.biz.vo.admin.ViewAppointmentPageReqVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentRespVO;
import com.wxy.rental.biz.vo.admin.ViewAppointmentUpdateStatusReqVO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 看房预约服务实现：列表批量回填公寓名，状态严格按流转表执行。
 *
 * <p>公寓名一次批量查出来再映射，避免逐行回查造成 N+1；姓名与手机用预约快照，不需要回查用户表。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalViewAppointmentServiceImpl implements RentalViewAppointmentService {

    /** 预约 Mapper */
    @Resource
    private RentalViewAppointmentMapper rentalViewAppointmentMapper;

    /** 公寓 Mapper：批量回填公寓名称 */
    @Resource
    private RentalApartmentMapper rentalApartmentMapper;

    /** 预约转换器 */
    @Resource
    private RentalViewAppointmentConvert rentalViewAppointmentConvert;

    /**
     * 分页查询看房预约列表
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageRespVO<ViewAppointmentRespVO> pageAppointment(ViewAppointmentPageReqVO reqVO) {
        Page<RentalViewAppointment> page = PageUtil.toPage(reqVO);
        IPage<RentalViewAppointment> result = rentalViewAppointmentMapper.selectAppointmentPage(page, reqVO);
        List<ViewAppointmentRespVO> records = rentalViewAppointmentConvert.toRespVOList(result.getRecords());
        if (records != null && !records.isEmpty()) {
            records.forEach(record -> record.setStatusName(
                    RentalAppointmentStatusEnum.labelOf(record.getStatus())));
            fillApartmentNames(records);
            // TODO wxy 等 infra 提供 App 用户批量查询接口（GET /internal-api/user/listByIds，返回 id / nickname / mobile）后回填 userNickname：收集本页 user_id 批量查一次再 set；见 docs/rental-api-contract.md 第 5 节第 5 项
        }
        return PageUtil.of(result, records);
    }

    /**
     * 预约状态流转
     *
     * @param reqVO 状态流转入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(ViewAppointmentUpdateStatusReqVO reqVO) {
        RentalViewAppointment po = reqVO.getId() == null ? null
                : rentalViewAppointmentMapper.selectById(reqVO.getId());
        if (po == null) {
            throw new BizException(RentalErrorConstant.APPOINTMENT_NOT_FOUND);
        }
        RentalAppointmentStatusEnum currentStatus = RentalAppointmentStatusEnum.of(po.getStatus());
        RentalAppointmentStatusEnum targetStatus = RentalAppointmentStatusEnum.of(reqVO.getStatus());
        if (currentStatus == null || !currentStatus.canTransitionTo(targetStatus)) {
            throw new BizException(RentalErrorConstant.APPOINTMENT_STATUS_TRANSITION_INVALID);
        }
        po.setStatus(targetStatus.getValue());
        rentalViewAppointmentMapper.updateById(po);
    }

    /**
     * 批量回填公寓名称
     *
     * @param records 预约返回体列表
     */
    private void fillApartmentNames(List<ViewAppointmentRespVO> records) {
        List<Long> apartmentIds = records.stream()
                .map(ViewAppointmentRespVO::getApartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (apartmentIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = rentalApartmentMapper.selectBatchIds(apartmentIds).stream()
                .collect(Collectors.toMap(RentalApartment::getId, RentalApartment::getName, (first, second) -> first));
        records.forEach(record -> record.setApartmentName(nameMap.get(record.getApartmentId())));
    }
}
