package com.wxy.ai.agent.biz.service;

import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentPageReqVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentRespVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchItemRespVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchReqVO;
import com.wxy.common.core.vo.PageRespVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档能力：上传解析入库、分页查询、重建索引、删除、检索调试。
 *
 * @author wxy
 * @date 2026/10/05
 */
public interface KnowledgeService {

    /**
     * 上传文档：保存原文件 → 解析切片 → 向量入库
     *
     * @param file 上传文件
     * @param city 城市标签，为空按平台级「通用」处理
     * @return 新文档 ID
     */
    Long upload(MultipartFile file, String city);

    /**
     * 分页查询文档
     *
     * @param reqVO 分页入参
     * @return 文档分页
     */
    PageRespVO<KnowledgeDocumentRespVO> page(KnowledgeDocumentPageReqVO reqVO);

    /**
     * 重建索引：删掉该文档的旧向量后重新解析入库
     *
     * @param id 文档 ID
     */
    void rebuild(Long id);

    /**
     * 删除文档：清理向量与原始文件，文档行逻辑删除
     *
     * @param id 文档 ID
     */
    void delete(Long id);

    /**
     * 语义检索调试
     *
     * @param reqVO 检索入参
     * @return 命中片段
     */
    List<KnowledgeSearchItemRespVO> search(KnowledgeSearchReqVO reqVO);
}
