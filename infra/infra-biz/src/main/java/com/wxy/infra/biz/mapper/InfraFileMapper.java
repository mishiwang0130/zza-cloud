package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 上传文件 Mapper：单表读写走 MyBatis-Plus，目前没有自定义 SQL。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface InfraFileMapper extends BaseMapper<InfraFile> {
}
