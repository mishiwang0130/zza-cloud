package com.wxy.ai.agent.biz.rag;

/**
 * 知识切片在向量库 metadata 里的字段名常量。
 *
 * <p>只保留检索与展示真正用到的字段：文档 ID（用于按文档删除向量）、文件名（回答引用来源）、
 * 城市（唯一过滤维度）、切片序号（排查时定位）。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class KnowledgeMetadataKeys {

    /** 知识库文档 ID，字符串形式 */
    public static final String DOCUMENT_ID = "documentId";

    /** 来源文件名 */
    public static final String FILE_NAME = "fileName";

    /** 城市标签：平台级通用文档为「通用」 */
    public static final String CITY = "city";

    /** 切片在文档内的序号，从 0 开始 */
    public static final String CHUNK_INDEX = "chunkIndex";

    /**
     * 工具类常量类，禁止实例化
     */
    private KnowledgeMetadataKeys() {
    }
}
