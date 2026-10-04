package com.wxy.rental.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.rental.biz.po.RentalImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 房源图片 Mapper：单表读写走 MyBatis-Plus，覆盖写前的物理删除写在
 * {@code resources/mapper/RentalImageMapper.xml}。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper
public interface RentalImageMapper extends BaseMapper<RentalImage> {

    /**
     * 物理删除某个对象下的全部图片（含历史上被逻辑删除的残留行）
     *
     * <p>覆盖写图片前必须先物理删除：逻辑删除只会把 {@code is_delete} 置 1，旧行还在表里，
     * 图片列表就会越查越多（历史行与本次行同时命中同一个对象 ID）。
     *
     * @param itemType 所属对象类型：1 公寓、2 房间
     * @param itemId   所属对象 ID
     * @return 删除行数
     */
    int deleteByItemTypeAndItemId(@Param("itemType") Integer itemType, @Param("itemId") Long itemId);
}
