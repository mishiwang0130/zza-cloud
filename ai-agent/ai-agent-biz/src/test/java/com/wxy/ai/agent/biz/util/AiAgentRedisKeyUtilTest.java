package com.wxy.ai.agent.biz.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.ai.agent.biz.constant.AiAgentRedisKeyConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Redis key 拼接单元测试：key 结构（全局前缀 + 模块前缀 + 业务段）是运维排查的约定，
 * 改动会被这里挡住。
 *
 * @author wxy
 * @date 2026/10/05
 */
class AiAgentRedisKeyUtilTest {

    /**
     * 会话锁 key：zza:ai-agent:chat:lock:{conversationId}
     */
    @Test
    @DisplayName("chatLockKey：拼出 zza:ai-agent:chat:lock:{会话ID}")
    void chatLockKeyShouldFollowConvention() {
        assertThat(AiAgentRedisKeyUtil.chatLockKey(1001L))
                .isEqualTo(AiAgentRedisKeyConstant.PREFIX + "chat:lock:1001")
                .startsWith("zza:ai-agent:");
    }

    /**
     * 文档锁 key：zza:ai-agent:knowledge:lock:{documentId}
     */
    @Test
    @DisplayName("knowledgeLockKey：拼出 zza:ai-agent:knowledge:lock:{文档ID}")
    void knowledgeLockKeyShouldFollowConvention() {
        assertThat(AiAgentRedisKeyUtil.knowledgeLockKey(20L))
                .isEqualTo(AiAgentRedisKeyConstant.PREFIX + "knowledge:lock:20")
                .startsWith("zza:ai-agent:");
    }
}
