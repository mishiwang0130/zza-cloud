package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraAppUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户端用户 Mapper：单表读写走 MyBatis-Plus；将来需要自定义 SQL 时，
 * 一律写在 {@code resources/mapper/InfraAppUserMapper.xml}，不写注解 SQL。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface InfraAppUserMapper extends BaseMapper<InfraAppUser> {
}