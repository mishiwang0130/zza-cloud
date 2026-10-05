package com.wxy.ai.agent.biz.service;

import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentPageReqVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeDocumentRespVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchItemRespVO;
import com.wxy.ai.agent.biz.vo.admin.KnowledgeSearchReqVO;
import com.wxy.ai.agent.biz.enums.AiAgentKnowledgeIndexActionEnum;
import com.wxy.common.core.vo.PageRespVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档能力：上传解析入库、分页查询、重建索引、删除、检索调试。
 *
 * <p>解析与向量化默认是异步的：上传 / 重建索引只把元数据与原始文件落好、投一条 MQ 消息就返回，
 * 真正的「解析切片 → 向量入库」由 {@link #executeIndexTask} 在消费者线程里执行。
 *
 * @author wxy
 * @date 2026/10/05
 */
public interface KnowledgeService {

    /**
     * 上传文档：保存元数据与原文件 → 投递索引任务 → 返回文档 ID
     *
     * <p>MQ 模式下新文档的状态是「待索引」，解析结果要刷新列表才能看到；
     * 投递失败时状态置「索引失败」并抛错，用户可稍后重建索引。
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
     * 重建索引：投递索引任务，由消费者删掉旧向量后重新解析入库
     *
     * @param id 文档 ID
     */
    void rebuild(Long id);

    /**
     * 执行一次索引任务：加文档锁 → 置「待索引」→（重建时）删旧向量 → 读原文件 → 解析向量化
     *
     * <p>由 MQ 消费者调用；{@code zza.ai-agent.knowledge.parse-mode=sync} 时上传 / 重建索引接口
     * 也会直接调它，保证两条链路走同一份解析逻辑。文档已被删除时记一条 warn 直接返回。
     *
     * @param documentId 文档 ID
     * @param action     动作：上传入库 / 重建索引
     */
    void executeIndexTask(Long documentId, AiAgentKnowledgeIndexActionEnum action);

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
