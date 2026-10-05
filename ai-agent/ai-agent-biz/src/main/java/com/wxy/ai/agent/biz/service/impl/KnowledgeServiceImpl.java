package com.wxy.ai.agent.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.ai.agent.biz.config.AiAgentProperties;
import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.ai.agent.biz.convert.AiAgentKnowledgeDocumentConvert;
import com.wxy.ai.agent.biz.enums.AiAgentDocumentStatusEnum;
import com.wxy.ai.agent.biz.mapper.AiAgentKnowledgeDocumentMapper;
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

        indexDocument(document, content);
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
        String lockKey = AiAgentRedisKeyUtil.knowledgeLockKey(id);
        if (!tryLock(lockKey)) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_DOCUMENT_BUSY);
        }
        try {
            updateStatus(document, AiAgentDocumentStatusEnum.PENDING, "");
            deleteVectors(id);
            byte[] content = readStoredFile(document.getStorageKey());
            indexDocument(document, content);
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
