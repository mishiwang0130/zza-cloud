package com.wxy.ai.agent.biz.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.ai.agent.biz.config.AiAgentProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 文档切片单元测试：切片参数来自配置，不需要模型与向量库。
 *
 * @author wxy
 * @date 2026/10/05
 */
class DocumentChunkerTest {

    /** 被测切片器 */
    private final DocumentChunker documentChunker = new DocumentChunker();

    /**
     * 用较小的切片参数，让单测跑得快且结果稳定
     */
    @BeforeEach
    void setUp() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getKnowledge().setChunkSize(200);
        properties.getKnowledge().setMinChunkSizeChars(50);
        properties.getKnowledge().setMinChunkLengthToEmbed(5);
        properties.getKnowledge().setMaxNumChunks(100);
        ReflectionTestUtils.setField(documentChunker, "properties", properties);
    }

    /**
     * 长文本切成多片，且每片都补上跨文档递增的 chunkIndex、保留原 metadata
     */
    @Test
    @DisplayName("切片：多片且带上 documentId 与 chunkIndex")
    void shouldSplitLongTextAndKeepMetadata() {
        String text = "押金为一个月租金，租金按月支付。退租需提前三十天提出申请，房屋无损坏时押金全额退还。"
                .repeat(20);
        Document paragraph = Document.builder()
                .text(text)
                .metadata(Map.of(
                        KnowledgeMetadataKeys.DOCUMENT_ID, "10",
                        KnowledgeMetadataKeys.FILE_NAME, "租赁规则.md",
                        KnowledgeMetadataKeys.CITY, "通用"))
                .build();

        List<Document> chunks = documentChunker.split(List.of(paragraph));

        assertThat(chunks).hasSizeGreaterThan(1);
        for (int index = 0; index < chunks.size(); index++) {
            Map<String, Object> metadata = chunks.get(index).getMetadata();
            assertThat(metadata)
                    .containsEntry(KnowledgeMetadataKeys.DOCUMENT_ID, "10")
                    .containsEntry(KnowledgeMetadataKeys.FILE_NAME, "租赁规则.md")
                    .containsEntry(KnowledgeMetadataKeys.CHUNK_INDEX, index);
            assertThat(chunks.get(index).getText()).isNotBlank();
        }
    }
}
