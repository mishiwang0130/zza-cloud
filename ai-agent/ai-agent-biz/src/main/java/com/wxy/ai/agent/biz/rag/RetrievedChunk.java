package com.wxy.ai.agent.biz.rag;

/**
 * 检索命中的一个知识片段。
 *
 * <p>这是 RAG 链路内部的数据结构（不是对外 VO）：向量库返回原始 {@code Document}，
 * 这里只保留业务用得到的五项，避免把整份 metadata 往上层传。
 *
 * @param documentId 知识库文档 ID（字符串形式）
 * @param fileName   来源文件名
 * @param city       城市标签
 * @param score      相似度得分，越大越相似
 * @param text       片段正文
 * @author wxy
 * @date 2026/10/05
 */
public record RetrievedChunk(String documentId, String fileName, String city, Double score, String text) {
}
