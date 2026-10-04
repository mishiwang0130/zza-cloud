package com.wxy.infra.biz.convert;

import com.wxy.infra.biz.po.InfraDictType;
import com.wxy.infra.biz.vo.admin.DictTypeRespVO;
import com.wxy.infra.biz.vo.admin.DictTypeSimpleRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 字典类型对象转换：实体到返回体。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InfraDictTypeConvert {

    /**
     * 实体转返回体
     *
     * @param po 字典类型实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    DictTypeRespVO toRespVO(InfraDictType po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 字典类型实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<DictTypeRespVO> toRespVOList(List<InfraDictType> list);

    /**
     * 实体转精简返回体（下拉用）
     *
     * @param po 字典类型实体，可以为 null
     * @return 精简返回体，入参为 null 时返回 null
     */
    DictTypeSimpleRespVO toSimpleRespVO(InfraDictType po);

    /**
     * 实体列表转精简返回体列表
     *
     * @param list 字典类型实体列表，可以为 null
     * @return 精简返回体列表，入参为 null 时返回 null
     */
    List<DictTypeSimpleRespVO> toSimpleRespVOList(List<InfraDictType> list);
}
