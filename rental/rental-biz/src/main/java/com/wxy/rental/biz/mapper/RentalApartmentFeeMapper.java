package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.rental.biz.po.RentalApartmentFee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 公寓费用项关联 Mapper：单表读写走 MyBatis-Plus，覆盖写前的物理删除写在
 * {@code resources/mapper/RentalApartmentFeeMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface RentalApartmentFeeMapper extends BaseMapper<RentalApartmentFee> {

    /**
     * 物理删除某个公寓的全部费用项关联（含历史上被逻辑删除的残留行）
     *
     * <p>覆盖写关联前必须先物理删除：逻辑删除只把 {@code is_delete} 置 1，旧行仍占着
     * {@code (apartment_id, fee_item_id)} 唯一键，重新关联同一个费用项会报 Duplicate entry。
     *
     * @param apartmentId 公寓 ID
     * @return 删除行数
     */
    int deleteByApartmentId(@Param("apartmentId") Long apartmentId);
}
