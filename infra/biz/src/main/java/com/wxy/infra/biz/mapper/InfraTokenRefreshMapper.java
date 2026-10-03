package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraTokenRefresh;
import org.apache.ibatis.annotations.Mapper;

/**
 * 续期凭证 Mapper：按摘要查记录、轮换失效等操作都用 MyBatis-Plus 的查询条件完成，无需自定义 SQL。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Mapper
public interface InfraTokenRefreshMapper extends BaseMapper<InfraTokenRefresh> {
}
