package com.wxy.infra.biz.convert;

import com.wxy.infra.biz.po.InfraDictData;
import com.wxy.infra.biz.vo.admin.DictDataRespVO;
import com.wxy.infra.biz.vo.admin.DictDataSimpleRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 字典数据对象转换：实体到返回体。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InfraDictDataConvert {

    /**
     * 实体转返回体
     *
     * @param po 字典数据实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    DictDataRespVO toRespVO(InfraDictData po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 字典数据实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<DictDataRespVO> toRespVOList(List<InfraDictData> list);

    /**
     * 实体列表转精简返回体列表（前端下拉用）
     *
     * @param list 字典数据实体列表，可以为 null
     * @return 精简返回体列表，入参为 null 时返回 null
     */
    List<DictDataSimpleRespVO> toSimpleRespVOList(List<InfraDictData> list);
}
