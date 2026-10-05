package com.wxy.ai.agent.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库文档索引状态：对应 {@code ai_agent_knowledge_document.status}。
 *
 * <p>上传后先落 PENDING，解析切片与向量化成功后置 INDEXED，失败置 FAILED 并记录原因；
 * 重建索引复用同一套状态流转。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Getter
@AllArgsConstructor
public enum AiAgentDocumentStatusEnum {

    /** 待索引：文件已保存，解析或向量化还没完成 */
    PENDING(0, "待索引"),

    /** 已索引：切片已写入向量库，可以被检索到 */
    INDEXED(1, "已索引"),

    /** 索引失败：解析或向量化出错，原因记在 error_message */
    FAILED(2, "索引失败");

    /** 入库值 */
    private final Integer value;

    /** 中文描述 */
    private final String label;

    /**
     * 按值查找枚举
     *
     * @param value 入库值，可以为 null
     * @return 匹配的枚举，找不到时返回 null
     */
    public static AiAgentDocumentStatusEnum of(Integer value) {
        for (AiAgentDocumentStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 取中文描述
     *
     * @param value 入库值，可以为 null
     * @return 中文描述，找不到时返回空串
     */
    public static String labelOf(Integer value) {
        AiAgentDocumentStatusEnum item = of(value);
        return item == null ? "" : item.label;
    }
}
