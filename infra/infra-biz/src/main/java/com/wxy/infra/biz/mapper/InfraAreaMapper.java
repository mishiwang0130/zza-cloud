package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraArea;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行政区划 Mapper：单表读写走 MyBatis-Plus（按 {@code parent_id} 取子级、按编码排序都在 Wrapper 里完成），
 * 目前没有自定义 SQL。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface InfraAreaMapper extends BaseMapper<InfraArea> {
}
