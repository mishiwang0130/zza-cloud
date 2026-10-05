package com.wxy.ai.agent.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.wxy.ai.agent.biz.rag.KnowledgeMetadataKeys;
import com.wxy.ai.agent.biz.service.KnowledgeService;
import com.wxy.ai.agent.biz.service.RagService;
import com.wxy.ai.agent.biz.storage.KnowledgeFileStorage;
import com.wxy.ai.agent.biz.util.AiAgentRedisKeyUtil;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentPageReqVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentRespVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchItemRespVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchReqVO;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.lock.util.DistributedLockUtil;
import com.wxy.common.mybatis.util.PageUtil;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档实现：上传与重建走同一条「解析 → 切片 → 向量入库」链路。
 *
 * <p><b>为什么上传方法不加事务</b>：索引失败时我们要保留文档行（status=FAILED + 失败原因），
 * 让后台能看到「哪篇文档、失败在哪一步」；如果整段放在一个事务里，抛异常会把这条记录一起回滚，
 * 失败信息反而查不到了。所以文档元数据、原文件、向量三段各自提交，失败状态单独写回。
 *
 * <p><b>解析为什么异步</b>：一篇文档的解析切片 + 向量化要调很多次模型，几十秒到几分钟都有可能，
 * 放在接口里用户只能干等（还会顶爆前端的请求超时）。所以上传与重建索引只做「落库 + 投递 MQ 消息」，
 * 真正的解析交给 {@link #executeIndexTask}，接口立刻返回、列表先显示「待索引」。
 * 本地没起 RocketMQ 时把 {@code zza.ai-agent.knowledge.parse-mode} 改成 sync 退回同步解析，两条链路共用同一份解析逻辑。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    /** 字节与 MB 的换算 */
    private static final long BYTES_PER_MB = 1024L * 1024L;

    /** 文档 Mapper */
    @Resource
    private AiAgentKnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 文档转换器 */
    @Resource
    private AiAgentKnowledgeDocumentConvert knowledgeDocumentConvert;

    /** 文档解析器工厂 */
    @Resource
    private DocumentParserFactory documentParserFactory;

    /** 文档切片器 */
    @Resource
    private DocumentChunker documentChunker;

    /** 原始文件存储（MinIO 或本机磁盘） */
    @Resource
    private KnowledgeFileStorage knowledgeFileStorage;

    /** 向量库：用 ObjectProvider 注入，未配置时给出明确错误 */
    @Resource
    private ObjectProvider<VectorStore> vectorStoreProvider;

    /** 分布式锁：同一文档的重建 / 删除必须串行 */
    @Resource
    private DistributedLockUtil distributedLockUtil;

    /** 业务配置 */
    @Resource
    private AiAgentProperties properties;

    /** 检索能力：检索调试直接复用它，避免两套检索逻辑 */
    @Resource
    private RagService ragService;

    /** 索引任务生产者：解析向量化改由消费者异步执行 */
    @Resource
    private AiAgentKnowledgeIndexProducer knowledgeIndexProducer;

    /**
     * 上传文档
     *
     * @param file 上传文件
     * @param city 城市标签，为空按「通用」处理
     * @return 新文档 ID
     */
    @Override
    public Long upload(MultipartFile file, String city) {
        if (file == null || file.isEmpty()) {
            throw new BizException(CommonErrorConstant.PARAM_ERROR, "上传文件不能为空");
        }
        AiAgentProperties.Knowledge knowledge = properties.getKnowledge();
        if (file.getSize() > knowledge.getMaxFileSizeMb() * BYTES_PER_MB) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_FILE_TOO_LARGE,
                    "文件不能超过 " + knowledge.getMaxFileSizeMb() + "MB");
        }
        byte[] content = readBytes(file);
        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "未命名文档";
        String cityTag = StringUtils.hasText(city) ? city.trim() : properties.getRag().getCommonCity();

        // 先把元数据落库拿到 ID：对象名与向量 metadata 都要用文档 ID 关联
        AiAgentKnowledgeDocument document = new AiAgentKnowledgeDocument();
        document.setFileName(fileName);
        document.setContentType(file.getContentType());
        document.setCity(cityTag);
        document.setFileSize(file.getSize());
        document.setChunkCount(0);
        document.setStatus(AiAgentDocumentStatusEnum.PENDING.getValue());
        document.setErrorMessage("");
        knowledgeDocumentMapper.insert(document);

        String objectName = knowledge.getStoragePrefix() + document.getId() + "/" + sanitizeFileName(fileName);
        knowledgeFileStorage.store(objectName, content, file.getContentType());
        document.setStorageKey(objectName);
        knowledgeDocumentMapper.updateById(document);

        // 解析与向量化不在这里做：MQ 模式只投一条消息，接口立刻返回，用户不用干等
        submitIndexTask(document, AiAgentKnowledgeIndexActionEnum.UPLOAD);
        return document.getId();
    }

    /**
     * 分页查询文档
     *
     * @param reqVO 分页入参
     * @return 文档分页
     */
    @Override
    public PageRespVO<KnowledgeDocumentRespVO> page(KnowledgeDocumentPageReqVO reqVO) {
        Page<AiAgentKnowledgeDocument> page = PageUtil.toPage(reqVO);
        IPage<AiAgentKnowledgeDocument> result = knowledgeDocumentMapper.selectPage(page,
                new LambdaQueryWrapper<AiAgentKnowledgeDocument>()
                        .like(StringUtils.hasText(reqVO.getFileName()),
                                AiAgentKnowledgeDocument::getFileName, reqVO.getFileName())
                        .eq(StringUtils.hasText(reqVO.getCity()),
                                AiAgentKnowledgeDocument::getCity, reqVO.getCity())
                        .eq(reqVO.getStatus() != null,
                                AiAgentKnowledgeDocument::getStatus, reqVO.getStatus())
                        .orderByDesc(AiAgentKnowledgeDocument::getId));
        List<KnowledgeDocumentRespVO> records = knowledgeDocumentConvert.toRespVOList(result.getRecords());
        records.forEach(record -> record.setStatusName(
                AiAgentDocumentStatusEnum.labelOf(record.getStatus())));
        return PageUtil.of(result, records);
    }

    /**
     * 重建索引
     *
     * @param id 文档 ID
     */
    @Override
    public void rebuild(Long id) {
        AiAgentKnowledgeDocument document = requireDocument(id);
        submitIndexTask(document, AiAgentKnowledgeIndexActionEnum.REBUILD);
    }

    /**
     * 执行一次索引任务
     *
     * @param documentId 文档 ID
     * @param action     动作：上传入库 / 重建索引
     */
    @Override
    public void executeIndexTask(Long documentId, AiAgentKnowledgeIndexActionEnum action) {
        AiAgentKnowledgeDocument document = documentId == null
                ? null : knowledgeDocumentMapper.selectById(documentId);
        if (document == null) {
            // 文档已被逻辑删除（或消息里的 ID 根本不存在）：这个任务已经没有意义，直接结束，不触发重试
            log.warn("索引任务对应的文档不存在，跳过：documentId={}, action={}", documentId, action);
            return;
        }
        String lockKey = AiAgentRedisKeyUtil.knowledgeLockKey(documentId);
        if (!tryLock(lockKey)) {
            // 正在被删除或另一次重建占用：抛出让 RocketMQ 延迟重试，比直接判失败更符合预期
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_DOCUMENT_BUSY);
        }
        try {
            updateStatus(document, AiAgentDocumentStatusEnum.PENDING, "");
            if (AiAgentKnowledgeIndexActionEnum.REBUILD == action) {
                // 重建必须先清旧向量：否则旧切片会和新切片一起被检索到
                deleteVectors(documentId);
            }
            indexDocument(document, readStoredFile(document.getStorageKey()));
        } finally {
            distributedLockUtil.unlock(lockKey);
        }
    }

    /**
     * 删除文档
     *
     * @param id 文档 ID
     */
    @Override
    public void delete(Long id) {
        AiAgentKnowledgeDocument document = requireDocument(id);
        String lockKey = AiAgentRedisKeyUtil.knowledgeLockKey(id);
        if (!tryLock(lockKey)) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_DOCUMENT_BUSY);
        }
        try {
            deleteVectors(id);
            if (StringUtils.hasText(document.getStorageKey())) {
                knowledgeFileStorage.delete(document.getStorageKey());
            }
            // 逻辑删除：文档行保留，便于追溯「这篇文档曾经存在过」
            knowledgeDocumentMapper.deleteById(id);
        } finally {
            distributedLockUtil.unlock(lockKey);
        }
    }

    /**
     * 语义检索调试
     *
     * @param reqVO 检索入参
     * @return 命中片段
     */
    @Override
    public List<KnowledgeSearchItemRespVO> search(KnowledgeSearchReqVO reqVO) {
        return ragService.search(reqVO.getQuery(), reqVO.getTopK(), reqVO.getCity());
    }

    /**
     * 解析切片并写入向量库
     *
     * @param document 文档实体
     * @param content  原始文件内容
     */
    private void indexDocument(AiAgentKnowledgeDocument document, byte[] content) {
        try {
            List<Document> paragraphs = documentParserFactory.parse(
                    document.getFileName(), document.getContentType(), content);
            List<Document> withMetadata = paragraphs.stream()
                    .map(paragraph -> Document.builder()
                            .text(paragraph.getText())
                            .metadata(buildMetadata(document))
                            .build())
                    .toList();
            List<Document> chunks = documentChunker.split(withMetadata);
            if (chunks.isEmpty()) {
                throw new BizException(AiAgentErrorConstant.KNOWLEDGE_DOCUMENT_PARSE_FAILED, "没有可用切片");
            }
            requireVectorStore().add(chunks);
            document.setChunkCount(chunks.size());
            updateStatus(document, AiAgentDocumentStatusEnum.INDEXED, "");
            log.info("知识库文档 {} 索引完成，切片 {} 片", document.getId(), chunks.size());
        } catch (RuntimeException ex) {
            String reason = ex.getMessage() == null ? "索引失败" : ex.getMessage();
            updateStatus(document, AiAgentDocumentStatusEnum.FAILED, reason);
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_INDEX_FAILED, reason, ex);
        }
    }

    /**
     * 提交索引任务：MQ 模式投消息后立刻返回，sync 模式当场解析（本地没起 RocketMQ 时的兜底）
     *
     * <p>投递失败会把文档置「索引失败」并抛出业务异常：状态与失败原因都落到用户能看到的地方，
     * 用户点「重建索引」即可重试，不会出现「接口报错了但文档一直显示待索引」的悬空状态。
     *
     * @param document 文档实体
     * @param action   动作：上传入库 / 重建索引
     */
    private void submitIndexTask(AiAgentKnowledgeDocument document, AiAgentKnowledgeIndexActionEnum action) {
        if (properties.getKnowledge().isSyncParseMode()) {
            executeIndexTask(document.getId(), action);
            return;
        }
        try {
            knowledgeIndexProducer.send(document.getId(), action);
        } catch (RuntimeException ex) {
            String reason = ex.getMessage() == null ? "索引任务投递失败" : ex.getMessage();
            updateStatus(document, AiAgentDocumentStatusEnum.FAILED, reason);
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_INDEX_MESSAGE_FAILED, reason, ex);
        }
        // 投递成功：立刻置「待索引」，后台列表能看到「处理中」，不必等消费者把状态改过来
        updateStatus(document, AiAgentDocumentStatusEnum.PENDING, "");
    }

    /**
     * 构造切片 metadata：只放检索与展示要用的字段
     *
     * @param document 文档实体
     * @return metadata
     */
    private Map<String, Object> buildMetadata(AiAgentKnowledgeDocument document) {
        Map<String, Object> metadata = new HashMap<>(4);
        metadata.put(KnowledgeMetadataKeys.DOCUMENT_ID, String.valueOf(document.getId()));
        metadata.put(KnowledgeMetadataKeys.FILE_NAME, document.getFileName());
        metadata.put(KnowledgeMetadataKeys.CITY, document.getCity());
        return metadata;
    }

    /**
     * 删除某文档的全部向量
     *
     * @param documentId 文档 ID
     */
    private void deleteVectors(Long documentId) {
        requireVectorStore().delete(new FilterExpressionBuilder()
                .eq(KnowledgeMetadataKeys.DOCUMENT_ID, String.valueOf(documentId))
                .build());
    }

    /**
     * 写回索引状态
     *
     * @param document     文档实体
     * @param status       状态
     * @param errorMessage 失败原因，成功时传空串
     */
    private void updateStatus(AiAgentKnowledgeDocument document,
                              AiAgentDocumentStatusEnum status, String errorMessage) {
        document.setStatus(status.getValue());
        document.setErrorMessage(errorMessage == null ? "" : errorMessage);
        knowledgeDocumentMapper.updateById(document);
    }

    /**
     * 按 ID 取文档
     *
     * @param id 文档 ID
     * @return 文档实体
     */
    private AiAgentKnowledgeDocument requireDocument(Long id) {
        AiAgentKnowledgeDocument document = id == null ? null : knowledgeDocumentMapper.selectById(id);
        if (document == null) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    /**
     * 抢文档锁
     *
     * @param lockKey 锁 key
     * @return 抢到返回 true
     */
    private boolean tryLock(String lockKey) {
        AiAgentProperties.Lock lock = properties.getLock();
        return distributedLockUtil.tryLock(lockKey, lock.getWaitMillis(), lock.getKnowledgeLeaseMillis());
    }

    /**
     * 取向量库，缺失时报明确错误
     *
     * @return 向量库
     */
    private VectorStore requireVectorStore() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_INDEX_FAILED,
                    "向量库未配置，请检查 spring.ai.vectorstore.qdrant.*");
        }
        return vectorStore;
    }

    /**
     * 读上传文件内容
     *
     * @param file 上传文件
     * @return 文件字节
     */
    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_DOCUMENT_PARSE_FAILED, "文件读取失败", ex);
        }
    }

    /**
     * 读回已保存的原始文件
     *
     * @param storageKey 对象名
     * @return 文件字节
     */
    private byte[] readStoredFile(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_INDEX_FAILED, "原始文件不存在，无法重建索引");
        }
        try (InputStream inputStream = knowledgeFileStorage.open(storageKey)) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_INDEX_FAILED, "原始文件读取失败", ex);
        }
    }

    /**
     * 清理文件名里的路径分隔符与非法字符
     *
     * @param fileName 原始文件名
     * @return 可安全用作对象名的文件名
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
