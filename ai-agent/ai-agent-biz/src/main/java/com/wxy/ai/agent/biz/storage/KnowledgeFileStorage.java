package com.wxy.ai.agent.biz.storage;

import java.io.InputStream;

/**
 * 知识库原始文件存储：把「文档原文件放哪」这件事收在一处。
 *
 * <p>两种实现：{@code MinioKnowledgeFileStorage}（默认，走 common-storage 的 MinioUtil）与
 * {@code LocalKnowledgeFileStorage}（没有对象存储时落本机磁盘，便于本地调试）。
 * 业务代码只依赖本接口，切换存储不用改 Service。
 *
 * @author wxy
 * @date 2026/10/05
 */
public interface KnowledgeFileStorage {

    /**
     * 保存文件
     *
     * @param objectName  对象名（含目录前缀），如 {@code ai-agent/documents/1001/规则.md}
     * @param content     文件内容
     * @param contentType 文件类型，如 {@code application/pdf}
     */
    void store(String objectName, byte[] content, String contentType);

    /**
     * 读取文件：重建索引时重新解析原文件
     *
     * @param objectName 对象名
     * @return 文件流，由调用方负责关闭
     */
    InputStream open(String objectName);

    /**
     * 删除文件：文档被删除时清理原始文件
     *
     * @param objectName 对象名
     */
    void delete(String objectName);
}
