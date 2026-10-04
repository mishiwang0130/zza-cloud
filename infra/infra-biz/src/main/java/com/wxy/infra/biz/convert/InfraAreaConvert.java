package com.wxy.infra.biz.convert;

import com.wxy.infra.biz.po.InfraArea;
import com.wxy.infra.api.dto.AreaDTO;
import com.wxy.infra.biz.vo.admin.AreaRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 行政区划对象转换：实体到返回体。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InfraAreaConvert {

    /**
     * 实体转返回体（children 由 Service 组装）
     *
     * @param po 区划实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    @Mapping(target = "children", ignore = true)
    AreaRespVO toRespVO(InfraArea po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 区划实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<AreaRespVO> toRespVOList(List<InfraArea> list);

    /**
     * 返回体转服务间 DTO（children 与入参同构，由实现自行递归映射）
     *
     * @param vo 区划返回体，可以为 null
     * @return DTO，入参为 null 时返回 null
     */
    AreaDTO toDTO(AreaRespVO vo);

    /**
     * 返回体列表转 DTO 列表
     *
     * @param list 区划返回体列表，可以为 null
     * @return DTO 列表，入参为 null 时返回 null
     */
    List<AreaDTO> toDTOList(List<AreaRespVO> list);
}
