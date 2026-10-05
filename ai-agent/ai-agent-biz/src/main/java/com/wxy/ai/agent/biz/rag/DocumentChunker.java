package com.wxy.ai.agent.biz.rag;

import com.wxy.ai.agent.biz.config.AiAgentProperties;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

/**
 * 文本切片：把「一篇文档」切成「若干片段（chunk）」。
 *
 * <p><b>为什么要切片</b>：模型与向量化都有输入长度上限；整篇文档只算一个向量会把多个主题平均掉，
 * 检索精度明显下降；检索的粒度就是切片的粒度，片段越干净，拼给模型的参考资料越准、越省 token。
 *
 * <p><b>切法</b>：{@code TokenTextSplitter} 按 token（CL100K_BASE 编码）切，尽量对齐标点，
 * 丢弃过短碎片，最多 {@code max-num-chunks} 片。它没有 overlap 参数，本项目也没有做重叠——
 * 片段边界的语义断裂靠「标点对齐」缓解，需要更强连续性时再考虑引入重叠或父子切片。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Component
public class DocumentChunker {

    /** 业务配置：切片参数都来自 zza.ai-agent.knowledge.*，调参不用改代码 */
    @Resource
    private AiAgentProperties properties;

    /**
     * 切分文档
     *
     * @param documents 解析器产出的文档（metadata 已包含 documentId / fileName / city）
     * @return 切片列表；每片补上跨文档统一的 chunkIndex，便于排查
     */
    public List<Document> split(List<Document> documents) {
        List<Document> chunks = buildSplitter().apply(documents);
        List<Document> result = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            Document chunk = chunks.get(index);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            // Spring AI 自己的 chunk_index 是「相对每个入参 Document」计数的，PDF 按页解析时会从 0 重新开始，
            // 这里统一覆盖成跨文档递增的序号：人工排查时能直接看出顺序
            metadata.put(KnowledgeMetadataKeys.CHUNK_INDEX, index);
            result.add(Document.builder().text(chunk.getText()).metadata(metadata).build());
        }
        return result;
    }

    /**
     * 用配置参数构造切片器
     *
     * @return 切片器
     */
    private TokenTextSplitter buildSplitter() {
        AiAgentProperties.Knowledge knowledge = properties.getKnowledge();
        return TokenTextSplitter.builder()
                .withChunkSize(knowledge.getChunkSize())
                .withMinChunkSizeChars(knowledge.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(knowledge.getMinChunkLengthToEmbed())
                .withMaxNumChunks(knowledge.getMaxNumChunks())
                .withKeepSeparator(knowledge.isKeepSeparator())
                .build();
    }
}
