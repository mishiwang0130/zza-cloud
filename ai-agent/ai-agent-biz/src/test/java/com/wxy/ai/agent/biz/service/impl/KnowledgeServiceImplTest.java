package com.wxy.ai.agent.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.ai.agent.biz.config.AiAgentProperties;
import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.ai.agent.biz.convert.AiAgentKnowledgeDocumentConvert;
import com.wxy.ai.agent.biz.enums.AiAgentDocumentStatusEnum;
import com.wxy.ai.agent.biz.enums.AiAgentKnowledgeIndexActionEnum;
import com.wxy.ai.agent.biz.mapper.AiAgentKnowledgeDocumentMapper;
import com.wxy.ai.agent.biz.mq.producer.AiAgentKnowledgeIndexProducer;
import com.wxy.ai.agent.biz.po.AiAgentKnowledgeDocument;
import com.wxy.ai.agent.biz.rag.DocumentChunker;
import com.wxy.ai.agent.biz.rag.DocumentParserFactory;
import com.wxy.ai.agent.biz.service.RagService;
import com.wxy.ai.agent.biz.storage.KnowledgeFileStorage;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.lock.util.DistributedLockUtil;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 知识库服务单元测试：mock Mapper、解析器与向量库，不连数据库、Redis、MQ 与模型。
 *
 * <p>重点守住「解析异步化」后的两条边界：接口只投消息不解析；投递失败必须把文档置成
 * 「索引失败」并把错误抛给调用方，不能让文档悬在「待索引」。
 *
 * @author wxy
 * @date 2026/10/05
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceImplTest {

    /** 文档 Mapper 替身 */
    @Mock
    private AiAgentKnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 文档转换器替身 */
    @Mock
    private AiAgentKnowledgeDocumentConvert knowledgeDocumentConvert;

    /** 文档解析器工厂替身 */
    @Mock
    private DocumentParserFactory documentParserFactory;

    /** 文档切片器替身 */
    @Mock
    private DocumentChunker documentChunker;

    /** 原文件存储替身 */
    @Mock
    private KnowledgeFileStorage knowledgeFileStorage;

    /** 向量库提供者替身 */
    @Mock
    private ObjectProvider<VectorStore> vectorStoreProvider;

    /** 向量库替身 */
    @Mock
    private VectorStore vectorStore;

    /** 分布式锁替身 */
    @Mock
    private DistributedLockUtil distributedLockUtil;

    /** 检索服务替身 */
    @Mock
    private RagService ragService;

    /** 索引任务生产者替身 */
    @Mock
    private AiAgentKnowledgeIndexProducer knowledgeIndexProducer;

    /** 被测服务 */
    private KnowledgeServiceImpl knowledgeService;

    /**
     * 装配被测服务：properties 用真实对象，默认就是 MQ 模式
     */
    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeServiceImpl();
        ReflectionTestUtils.setField(knowledgeService, "knowledgeDocumentMapper", knowledgeDocumentMapper);
        ReflectionTestUtils.setField(knowledgeService, "knowledgeDocumentConvert", knowledgeDocumentConvert);
        ReflectionTestUtils.setField(knowledgeService, "documentParserFactory", documentParserFactory);
        ReflectionTestUtils.setField(knowledgeService, "documentChunker", documentChunker);
        ReflectionTestUtils.setField(knowledgeService, "knowledgeFileStorage", knowledgeFileStorage);
        ReflectionTestUtils.setField(knowledgeService, "vectorStoreProvider", vectorStoreProvider);
        ReflectionTestUtils.setField(knowledgeService, "distributedLockUtil", distributedLockUtil);
        ReflectionTestUtils.setField(knowledgeService, "ragService", ragService);
        ReflectionTestUtils.setField(knowledgeService, "knowledgeIndexProducer", knowledgeIndexProducer);
        ReflectionTestUtils.setField(knowledgeService, "properties", new AiAgentProperties());
    }

    /**
     * MQ 模式下上传只落库 + 投消息，不在接口线程里解析
     */
    @Test
    @DisplayName("upload：MQ 模式只投递索引任务，不同步解析")
    void uploadShouldPublishIndexTaskInMqMode() {
        mockInsertAssignId(11L);
        MockMultipartFile file = buildFile("租赁规则.md");

        Long documentId = knowledgeService.upload(file, null);

        assertThat(documentId).isEqualTo(11L);
        verify(knowledgeIndexProducer).send(11L, AiAgentKnowledgeIndexActionEnum.UPLOAD);
        verify(documentParserFactory, never()).parse(anyString(), any(), any());
    }

    /**
     * 消息投不出去时文档不能停在「待索引」：状态置「索引失败」并抛出可读的业务异常
     */
    @Test
    @DisplayName("upload：投递失败时标记索引失败并抛错")
    void uploadShouldMarkFailedWhenPublishFails() {
        mockInsertAssignId(11L);
        doThrow(new IllegalStateException("RocketMQTemplate 不存在"))
                .when(knowledgeIndexProducer).send(anyLong(), any());
        MockMultipartFile file = buildFile("租赁规则.md");

        assertThatThrownBy(() -> knowledgeService.upload(file, null))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getCode())
                .isEqualTo(AiAgentErrorConstant.KNOWLEDGE_INDEX_MESSAGE_FAILED.code());

        assertThat(lastUpdatedDocument().getStatus())
                .isEqualTo(AiAgentDocumentStatusEnum.FAILED.getValue());
        assertThat(lastUpdatedDocument().getErrorMessage()).contains("RocketMQTemplate 不存在");
    }

    /**
     * 重建索引同样只投消息：文档锁由消费者拿，接口线程不加锁
     */
    @Test
    @DisplayName("rebuild：MQ 模式投递任务并把状态置为待索引")
    void rebuildShouldPublishAndMarkPendingInMqMode() {
        AiAgentKnowledgeDocument document = buildStoredDocument(5L);
        document.setStatus(AiAgentDocumentStatusEnum.INDEXED.getValue());
        when(knowledgeDocumentMapper.selectById(5L)).thenReturn(document);

        knowledgeService.rebuild(5L);

        verify(knowledgeIndexProducer).send(5L, AiAgentKnowledgeIndexActionEnum.REBUILD);
        assertThat(lastUpdatedDocument().getStatus())
                .isEqualTo(AiAgentDocumentStatusEnum.PENDING.getValue());
        verifyNoInteractions(distributedLockUtil);
    }

    /**
     * 文档已被删除（消息在队列里排着队，用户先点了删除）时任务直接结束，不重试
     */
    @Test
    @DisplayName("executeIndexTask：文档不存在时直接跳过")
    void executeIndexTaskShouldSkipMissingDocument() {
        when(knowledgeDocumentMapper.selectById(9L)).thenReturn(null);

        knowledgeService.executeIndexTask(9L, AiAgentKnowledgeIndexActionEnum.UPLOAD);

        verifyNoInteractions(distributedLockUtil, knowledgeIndexProducer);
    }

    /**
     * 消费者执行重建任务：先删旧向量，再解析入库并回写「已索引」与切片数
     */
    @Test
    @DisplayName("executeIndexTask：重建先删旧向量再入库")
    void executeIndexTaskShouldDeleteOldVectorsBeforeIndexing() {
        AiAgentKnowledgeDocument document = buildStoredDocument(5L);
        when(knowledgeDocumentMapper.selectById(5L)).thenReturn(document);
        when(distributedLockUtil.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(knowledgeFileStorage.open("ai-agent/documents/5/租赁规则.md"))
                .thenReturn(new ByteArrayInputStream("# 押金".getBytes(StandardCharsets.UTF_8)));
        when(documentParserFactory.parse(anyString(), any(), any()))
                .thenReturn(List.of(new Document("段落正文")));
        when(documentChunker.split(anyList())).thenReturn(List.of(new Document("切片正文")));
        when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);

        knowledgeService.executeIndexTask(5L, AiAgentKnowledgeIndexActionEnum.REBUILD);

        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(vectorStore).add(anyList());
        verify(distributedLockUtil).unlock(anyString());
        assertThat(document.getStatus()).isEqualTo(AiAgentDocumentStatusEnum.INDEXED.getValue());
        assertThat(document.getChunkCount()).isEqualTo(1);
    }

    /**
     * 上传入库不需要删向量：向量库里本来就没有这篇文档的切片
     */
    @Test
    @DisplayName("executeIndexTask：上传入库不删向量")
    void executeIndexTaskShouldNotDeleteVectorsOnUpload() {
        AiAgentKnowledgeDocument document = buildStoredDocument(6L);
        when(knowledgeDocumentMapper.selectById(6L)).thenReturn(document);
        when(distributedLockUtil.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(knowledgeFileStorage.open(anyString()))
                .thenReturn(new ByteArrayInputStream("正文".getBytes(StandardCharsets.UTF_8)));
        when(documentParserFactory.parse(anyString(), any(), any()))
                .thenReturn(List.of(new Document("段落正文")));
        when(documentChunker.split(anyList())).thenReturn(List.of(new Document("切片正文")));
        when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);

        knowledgeService.executeIndexTask(6L, AiAgentKnowledgeIndexActionEnum.UPLOAD);

        verify(vectorStore, never()).delete(any(Filter.Expression.class));
        verify(vectorStore).add(anyList());
    }

    /**
     * 让 insert 回填主键：真实环境里由数据库自增生成，服务要用它拼对象名与消息
     *
     * @param documentId 回填的文档 ID
     */
    private void mockInsertAssignId(Long documentId) {
        when(knowledgeDocumentMapper.insert(any(AiAgentKnowledgeDocument.class))).thenAnswer(invocation -> {
            AiAgentKnowledgeDocument document = invocation.getArgument(0);
            document.setId(documentId);
            return 1;
        });
    }

    /**
     * 构造一个已落库的文档（含原文件对象名）
     *
     * @param documentId 文档 ID
     * @return 文档实体
     */
    private AiAgentKnowledgeDocument buildStoredDocument(Long documentId) {
        AiAgentKnowledgeDocument document = new AiAgentKnowledgeDocument();
        document.setId(documentId);
        document.setFileName("租赁规则.md");
        document.setContentType("text/markdown");
        document.setCity("通用");
        document.setStorageKey("ai-agent/documents/" + documentId + "/租赁规则.md");
        document.setChunkCount(0);
        document.setStatus(AiAgentDocumentStatusEnum.PENDING.getValue());
        return document;
    }

    /**
     * 构造上传文件
     *
     * @param fileName 原始文件名
     * @return 上传文件
     */
    private MockMultipartFile buildFile(String fileName) {
        return new MockMultipartFile("file", fileName, "text/markdown",
                "# 押金".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 取最后一次 updateById 的入参：状态回写都是整实体更新
     *
     * @return 最后被更新的文档
     */
    private AiAgentKnowledgeDocument lastUpdatedDocument() {
        ArgumentCaptor<AiAgentKnowledgeDocument> captor =
                ArgumentCaptor.forClass(AiAgentKnowledgeDocument.class);
        verify(knowledgeDocumentMapper, atLeastOnce()).updateById(captor.capture());
        List<AiAgentKnowledgeDocument> values = captor.getAllValues();
        return values.get(values.size() - 1);
    }
}
