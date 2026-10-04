package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.rental.biz.po.RentalFeeItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用项 Mapper：单表读写与名称查重都在 MyBatis-Plus 的 Wrapper 里完成，目前没有自定义 SQL。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface RentalFeeItemMapper extends BaseMapper<RentalFeeItem> {
}
