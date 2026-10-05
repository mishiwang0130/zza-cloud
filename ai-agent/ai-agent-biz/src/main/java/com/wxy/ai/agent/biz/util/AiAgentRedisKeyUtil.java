package com.wxy.ai.agent.biz.util;

import com.wxy.ai.agent.biz.constant.AiAgentRedisKeyConstant;

/**
 * ai-agent 的 Redis key 拼接工具：一个 key 一个方法，方法内部自己拼。
 *
 * <p>刻意不提供通用的 {@code buildKey(prefix, id)}：把「key 长什么样」推给调用方，
 * key 结构一改就要满仓库找调用点。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class AiAgentRedisKeyUtil {

    /**
     * 同一会话并发提问的互斥锁 key
     *
     * @param conversationId 会话 ID
     * @return {@code zza:ai-agent:chat:lock:{conversationId}}
     */
    public static String chatLockKey(Long conversationId) {
        return AiAgentRedisKeyConstant.CHAT_LOCK + conversationId;
    }

    /**
     * 同一知识文档重建 / 删除的互斥锁 key
     *
     * @param documentId 文档 ID
     * @return {@code zza:ai-agent:knowledge:lock:{documentId}}
     */
    public static String knowledgeLockKey(Long documentId) {
        return AiAgentRedisKeyConstant.KNOWLEDGE_LOCK + documentId;
    }

    /**
     * 工具类，禁止实例化
     */
    private AiAgentRedisKeyUtil() {
    }
}
