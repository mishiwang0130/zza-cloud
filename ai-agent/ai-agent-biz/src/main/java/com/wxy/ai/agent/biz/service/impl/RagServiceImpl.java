package com.wxy.ai.agent.biz.service.impl;

import com.wxy.ai.agent.biz.config.AiAgentProperties;
import com.wxy.ai.agent.biz.rag.CityFilter;
import com.wxy.ai.agent.biz.rag.KnowledgeMetadataKeys;
import com.wxy.ai.agent.biz.rag.RetrievalResult;
import com.wxy.ai.agent.biz.rag.RetrievedChunk;
import com.wxy.ai.agent.biz.service.RagService;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchItemRespVO;
import com.wxy.ai.agent.biz.vo.app.ChatSourceRespVO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 知识库检索实现：向量检索 → 城市过滤（必要时回退）→ 拼参考资料与提示词。
 *
 * <p><b>降级策略</b>：检索只是增强，不是刚性依赖。向量库没装配、Embedding 调用失败、
 * Qdrant 连不上时，只要 {@code fail-fast=false} 就记一条 warn 并返回「无参考资料」，
 * 让本轮退化成纯模型回答；用户至少能拿到回复，而不是一个错误。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    /** 来源摘要最大长度：只用于前端展示与后台排查，不参与提示词 */
    private static final int SNIPPET_MAX_LENGTH = 120;

    /** 提示词里的时间格式 */
    private static final DateTimeFormatter PROMPT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 向量库：用 ObjectProvider 注入，测试环境可以没有这个 Bean */
    @Resource
    private ObjectProvider<VectorStore> vectorStoreProvider;

    /** 业务配置 */
    @Resource
    private AiAgentProperties properties;

    /**
     * 按问题检索知识库
     *
     * @param question 用户本轮问题
     * @param city     选中的城市标签，可为空
     * @return 检索结果
     */
    @Override
    public RetrievalResult retrieve(String question, String city) {
        AiAgentProperties.Rag rag = properties.getRag();
        if (!rag.isEnabled() || !StringUtils.hasText(question)) {
            return RetrievalResult.empty(false);
        }
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.debug("未装配 VectorStore，本轮跳过知识库检索");
            return RetrievalResult.empty(true);
        }
        try {
            Filter.Expression cityFilter = CityFilter.build(city, rag.getCityMetadataKey(), rag.getCommonCity());
            List<Document> documents = search(vectorStore, question, rag.getTopK(),
                    rag.getSimilarityThreshold(), cityFilter);
            if ((documents == null || documents.isEmpty())
                    && cityFilter != null && rag.isFallbackToUnfilteredWhenEmpty()) {
                // 城市过滤后一条都没有，很可能是文档城市标签没打对：宁可放宽范围，也别让用户拿不到任何参考资料
                log.debug("城市 {} 过滤后没有命中，回退为不带过滤的检索", city);
                documents = search(vectorStore, question, rag.getTopK(), rag.getSimilarityThreshold(), null);
            }
            if (documents == null || documents.isEmpty()) {
                return RetrievalResult.empty(false);
            }
            return toRetrievalResult(documents, rag.getMaxContextChars());
        } catch (Exception ex) {
            if (rag.isFailFast()) {
                throw ex;
            }
            log.warn("知识库检索失败，本轮降级为纯模型回答：{}", ex.getMessage());
            return RetrievalResult.empty(true);
        }
    }

    /**
     * 拼本轮用户消息
     *
     * <p>当前时间直接拼进提示词而不是做成工具：模型需要它只是为了判断今天几号、预约时间是否合理，
     * 为一次简单取时间多跑一轮工具调用不值得。
     *
     * @param question  用户问题
     * @param city      城市标签，可为空
     * @param retrieval 检索结果
     * @return 用户消息文本
     */
    @Override
    public String buildUserPrompt(String question, String city, RetrievalResult retrieval) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("当前时间：").append(LocalDateTime.now().format(PROMPT_TIME_FORMATTER)).append('\n');
        if (StringUtils.hasText(city)) {
            prompt.append("用户所在城市：").append(city.trim()).append('\n');
        }
        prompt.append("用户问题：\n").append(question);
        if (retrieval != null && retrieval.hasContext()) {
            prompt.append("\n\n参考资料（可能不完整，请结合资料回答；引用时说明来源文件名，资料里没有的信息不要编造）：\n")
                    .append(retrieval.contextText());
        }
        return prompt.toString();
    }

    /**
     * 命中片段转对外来源结构
     *
     * @param chunks 命中片段
     * @return 来源列表
     */
    @Override
    public List<ChatSourceRespVO> toSourceList(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        return chunks.stream().map(chunk -> {
            ChatSourceRespVO source = new ChatSourceRespVO();
            source.setDocumentId(chunk.documentId());
            source.setFileName(chunk.fileName());
            source.setScore(chunk.score());
            source.setSnippet(snippet(chunk.text()));
            return source;
        }).toList();
    }

    /**
     * 语义检索调试
     *
     * @param query 检索问题
     * @param topK  召回条数，为空取配置默认值
     * @param city  城市标签，可为空
     * @return 命中片段列表
     */
    @Override
    public List<KnowledgeSearchItemRespVO> search(String query, Integer topK, String city) {
        AiAgentProperties.Rag rag = properties.getRag();
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return List.of();
        }
        int size = topK == null ? rag.getTopK() : topK;
        // 调试接口刻意不设相似度阈值：排查「为什么没召回」时，看到低分片段比看到空列表有用
        List<Document> documents = search(vectorStore, query, size,
                SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL,
                CityFilter.build(city, rag.getCityMetadataKey(), rag.getCommonCity()));
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<KnowledgeSearchItemRespVO> items = new ArrayList<>(documents.size());
        for (Document document : documents) {
            KnowledgeSearchItemRespVO item = new KnowledgeSearchItemRespVO();
            item.setDocumentId(asText(document.getMetadata().get(KnowledgeMetadataKeys.DOCUMENT_ID), null));
            item.setFileName(asText(document.getMetadata().get(KnowledgeMetadataKeys.FILE_NAME), null));
            item.setCity(asText(document.getMetadata().get(KnowledgeMetadataKeys.CITY), null));
            item.setScore(document.getScore());
            item.setText(document.getText());
            items.add(item);
        }
        return items;
    }

    /**
     * 调用向量库做一次相似度检索
     *
     * @param vectorStore         向量库
     * @param query               查询文本
     * @param topK                召回条数
     * @param similarityThreshold 相似度阈值
     * @param filter              过滤表达式，可为 null
     * @return 命中的文档
     */
    private List<Document> search(VectorStore vectorStore, String query, int topK,
                                  double similarityThreshold, Filter.Expression filter) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);
        if (filter != null) {
            builder.filterExpression(filter);
        }
        return vectorStore.similaritySearch(builder.build());
    }

    /**
     * 组装检索结果：参考资料文本 + 结构化片段
     *
     * @param documents       命中的文档
     * @param maxContextChars 参考资料最大字符数
     * @return 检索结果
     */
    private RetrievalResult toRetrievalResult(List<Document> documents, int maxContextChars) {
        List<RetrievedChunk> chunks = new ArrayList<>(documents.size());
        StringBuilder context = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            String fileName = asText(metadata.get(KnowledgeMetadataKeys.FILE_NAME), "未命名文档");
            String section = "[%d] 来源文件：%s\n%s\n\n".formatted(index, fileName, document.getText());
            if (context.length() + section.length() > maxContextChars) {
                // 放不下就停止追加：宁可少给两段资料，也不要截断半个片段或把提示词顶爆
                break;
            }
            context.append(section);
            chunks.add(new RetrievedChunk(
                    asText(metadata.get(KnowledgeMetadataKeys.DOCUMENT_ID), null),
                    fileName,
                    asText(metadata.get(KnowledgeMetadataKeys.CITY), null),
                    document.getScore(),
                    document.getText()));
            index++;
        }
        return new RetrievalResult(List.copyOf(chunks), context.toString().trim(), false);
    }

    /**
     * 生成展示用摘要
     *
     * @param text 片段正文
     * @return 单行摘要
     */
    private String snippet(String text) {
        if (text == null) {
            return "";
        }
        String flattened = text.replaceAll("\\s+", " ").trim();
        return flattened.length() <= SNIPPET_MAX_LENGTH
                ? flattened
                : flattened.substring(0, SNIPPET_MAX_LENGTH) + "…";
    }

    /**
     * metadata 取值转字符串
     *
     * @param value        metadata 值
     * @param defaultValue 默认值
     * @return 字符串值
     */
    private String asText(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
