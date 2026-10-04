package com.wxy.infra.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.infra.biz.po.InfraDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典数据 Mapper：单表读写走 MyBatis-Plus，批量刷类型编码写在
 * {@code resources/mapper/InfraDictDataMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface InfraDictDataMapper extends BaseMapper<InfraDictData> {

    /**
     * 把某个字典类型编码下的数据整体改成新编码（改类型编码时用）
     *
     * <p>自定义 UPDATE 不会触发 MyBatis-Plus 的审计填充，所以这里显式更新 update_time / update_by。
     *
     * @param oldType  原类型编码
     * @param newType  新类型编码
     * @param updateBy 操作人 ID，0 表示系统或未登录
     * @return 更新行数
     */
    int updateDictType(@Param("oldType") String oldType,
                       @Param("newType") String newType,
                       @Param("updateBy") Long updateBy);
}
