package com.wxy.ai.agent.biz.service;

import com.wxy.ai.agent.biz.rag.RetrievalResult;
import com.wxy.ai.agent.biz.rag.RetrievedChunk;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchItemRespVO;
import com.wxy.ai.agent.biz.vo.app.ChatSourceRespVO;
import java.util.List;

/**
 * 知识库检索（RAG）能力：只负责「查资料 + 拼提示词」，不负责入库与文档管理。
 *
 * @author wxy
 * @date 2026/10/05
 */
public interface RagService {

    /**
     * 按问题检索知识库
     *
     * @param question 用户本轮问题
     * @param city     选中的城市标签，可为空
     * @return 检索结果；检索不可用时返回降级结果而不是抛异常（fail-fast 打开时除外）
     */
    RetrievalResult retrieve(String question, String city);

    /**
     * 把「当前时间 + 城市 + 问题 + 参考资料」拼成本轮用户消息
     *
     * @param question  用户本轮问题
     * @param city      选中的城市标签，可为空
     * @param retrieval 检索结果
     * @return 拼好的用户消息文本
     */
    String buildUserPrompt(String question, String city, RetrievalResult retrieval);

    /**
     * 把命中的片段转成对外来源结构（小程序端展示「回答依据」）
     *
     * @param chunks 命中的片段
     * @return 来源列表
     */
    List<ChatSourceRespVO> toSourceList(List<RetrievedChunk> chunks);

    /**
     * 语义检索调试：返回完整片段正文，供管理端判断检索质量
     *
     * @param query 检索问题
     * @param topK  召回条数，为空取配置默认值
     * @param city  城市标签，可为空
     * @return 命中片段列表
     */
    List<KnowledgeSearchItemRespVO> search(String query, Integer topK, String city);
}
