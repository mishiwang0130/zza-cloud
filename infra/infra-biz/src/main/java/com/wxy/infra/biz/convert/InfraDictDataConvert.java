package com.wxy.infra.biz.convert;

import com.wxy.infra.biz.po.InfraDictData;
import com.wxy.infra.api.dto.DictDataSimpleDTO;
import com.wxy.infra.biz.vo.admin.DictDataRespVO;
import com.wxy.infra.biz.vo.admin.DictDataSimpleRespVO;
import com.wxy.infra.biz.vo.app.DictDataAppRespVO;
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

    /**
     * 实体列表转用户端返回体列表（只含标签与值）
     *
     * @param list 字典数据实体列表，可以为 null
     * @return 用户端返回体列表，入参为 null 时返回 null
     */
    List<DictDataAppRespVO> toAppRespVOList(List<InfraDictData> list);

    /**
     * 精简返回体列表转服务间 DTO 列表
     *
     * <p>只映射标签与值两项：服务间接口是给别的服务回填展示文案用的，
     * 状态、排序号这些维护端字段不外发。
     *
     * @param list 精简返回体列表，可以为 null
     * @return DTO 列表，入参为 null 时返回 null
     */
    List<DictDataSimpleDTO> toSimpleDTOList(List<DictDataSimpleRespVO> list);
}
