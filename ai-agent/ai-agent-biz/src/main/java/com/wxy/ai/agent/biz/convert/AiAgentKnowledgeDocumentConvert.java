package com.wxy.ai.agent.biz.convert;

import com.wxy.ai.agent.biz.po.AiAgentKnowledgeDocument;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 知识库文档对象转换：实体到管理端返回体。
 *
 * <p>状态中文名由 Service 按枚举回填，这里 {@code ignore}。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiAgentKnowledgeDocumentConvert {

    /**
     * 实体转返回体
     *
     * @param po 文档实体，可以为 null
     * @return 返回体，入参为 null 时返回 null
     */
    @Mapping(target = "statusName", ignore = true)
    KnowledgeDocumentRespVO toRespVO(AiAgentKnowledgeDocument po);

    /**
     * 实体列表转返回体列表
     *
     * @param list 文档实体列表，可以为 null
     * @return 返回体列表，入参为 null 时返回 null
     */
    List<KnowledgeDocumentRespVO> toRespVOList(List<AiAgentKnowledgeDocument> list);
}
