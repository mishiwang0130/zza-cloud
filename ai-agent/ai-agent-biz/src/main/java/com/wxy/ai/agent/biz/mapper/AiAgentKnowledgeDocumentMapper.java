package com.wxy.ai.agent.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxy.ai.agent.biz.po.AiAgentKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档表 Mapper。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Mapper
public interface AiAgentKnowledgeDocumentMapper extends BaseMapper<AiAgentKnowledgeDocument> {
}
