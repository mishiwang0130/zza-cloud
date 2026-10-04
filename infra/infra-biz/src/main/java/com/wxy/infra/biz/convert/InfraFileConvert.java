package com.wxy.infra.biz.convert;

import com.wxy.infra.api.dto.FileRespDTO;
import com.wxy.infra.biz.vo.FileRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 文件对象转换：服务返回体转服务间 DTO。
 *
 * <p>字段一一对应，映射本身没有逻辑；单独留一层是为了让「哪些字段可以对外」这件事有明确落点——
 * 将来服务返回体加了内部字段，不会顺着默认映射偷偷发给其他服务。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InfraFileConvert {

    /**
     * 返回体转服务间 DTO
     *
     * @param vo 文件返回体，可以为 null
     * @return DTO，入参为 null 时返回 null
     */
    FileRespDTO toDTO(FileRespVO vo);

    /**
     * 返回体列表转 DTO 列表
     *
     * @param list 文件返回体列表，可以为 null
     * @return DTO 列表，入参为 null 时返回 null
     */
    List<FileRespDTO> toDTOList(List<FileRespVO> list);
}
