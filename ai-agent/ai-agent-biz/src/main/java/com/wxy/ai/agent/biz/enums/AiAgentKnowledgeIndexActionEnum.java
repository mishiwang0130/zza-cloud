package com.wxy.ai.agent.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库索引任务的动作：决定消费者对这篇文档做什么。
 *
 * <p>上传只做「解析切片 → 向量入库」；重建索引还要先把旧向量删掉，
 * 否则旧切片会和新切片一起被检索到。两者的解析链路完全相同，所以共用一个消息体与消费者，
 * 靠这个动作区分要不要先清向量。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Getter
@AllArgsConstructor
public enum AiAgentKnowledgeIndexActionEnum {

    /** 上传入库：文档行与原文件刚落地，向量库里还没有这篇文档的切片 */
    UPLOAD(1, "上传入库"),

    /** 重建索引：解析入库前要先删掉该文档的旧向量 */
    REBUILD(2, "重建索引");

    /** 入库值：只用于日志与排查，消息体里传的是枚举名 */
    private final Integer value;

    /** 中文描述 */
    private final String label;

    /**
     * 按枚举名查找动作
     *
     * @param name 枚举名，大小写敏感，可为 null
     * @return 匹配的枚举，找不到时返回 null（由调用方决定怎么报错）
     */
    public static AiAgentKnowledgeIndexActionEnum ofName(String name) {
        for (AiAgentKnowledgeIndexActionEnum item : values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }
}
