package com.wxy.ai.agent.biz.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话记忆装配：把框架提供的 Redis 仓库装进 {@code MessageWindowChatMemory}。
 *
 * <p><b>为什么不直接用框架的 {@code ChatMemoryAutoConfiguration}</b>：它除了按仓库创建
 * {@code ChatMemory}，还会额外注册一个名字固定的「进程内兜底」{@code ChatMemoryRepository}；
 * 在 Redis 记忆同时启用时容器里会有两个同类型仓库，{@code chatMemory} 方法注入时直接报
 * 「expected single matching bean but found 2」。所以这里关掉它的总开关
 * （{@code spring.ai.memory.enabled=false}），只保留 Redis 仓库的自动配置，自己组装窗口——
 * 存储、序列化、key 前缀仍然全部是框架实现，我们只决定「保留最近多少条」。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Configuration
public class AiAgentChatMemoryConfiguration {

    /**
     * 会话记忆：Redis 仓库 + 消息窗口
     *
     * @param chatMemoryRepository Redis 会话记忆仓库（由框架的 Redis 记忆自动配置提供）
     * @param maxMessages          窗口大小，与 {@code spring.ai.memory.max-messages} 保持一致
     * @return 会话记忆
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository,
                                 @Value("${spring.ai.memory.max-messages:20}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }
}
