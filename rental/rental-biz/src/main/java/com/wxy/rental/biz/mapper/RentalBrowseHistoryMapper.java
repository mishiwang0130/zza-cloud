package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.rental.biz.po.RentalBrowseHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 浏览记录 Mapper：写入（纯插入）与按用户分页都在 MyBatis-Plus 的 Wrapper 里完成，
 * 目前没有自定义 SQL。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface RentalBrowseHistoryMapper extends BaseMapper<RentalBrowseHistory> {
}
