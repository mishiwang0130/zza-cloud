package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理后台用户 Mapper：单表读写走 MyBatis-Plus，自定义 SQL 一律写在 {@code resources/mapper/InfraUserMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper
public interface InfraUserMapper extends BaseMapper<InfraUser> {
}
