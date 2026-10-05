package com.wxy.ai.agent.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档表 {@code ai_agent_knowledge_document} 的实体。
 *
 * <p>只记录文档元数据与索引状态；切片正文与向量存在 Qdrant 里，按 {@code documentId} 关联。
 * {@code city} 是唯一的过滤维度：检索时按「选中城市 + 通用」过滤。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_knowledge_document")
public class AiAgentKnowledgeDocument extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 原始文件名：回答里引用来源时展示它 */
    private String fileName;

    /** 文件类型（Content-Type），如 application/pdf */
    private String contentType;

    /** 城市标签：平台级通用文档填「通用」 */
    private String city;

    /** 文件字节数 */
    private Long fileSize;

    /** 原始文件在对象存储 / 本地磁盘里的对象名 */
    private String storageKey;

    /** 切片数量：索引成功后回写 */
    private Integer chunkCount;

    /** 索引状态：0 待索引、1 已索引、2 索引失败 */
    private Integer status;

    /** 索引失败原因：只在 status=2 时有值 */
    private String errorMessage;
}
