package com.wxy.ai.agent.biz;

import com.wxy.common.lock.util.DistributedLockUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 应用装配测试：用内存库与替身启动整个上下文，验证「装配层面」不会炸。
 *
 * <p>它守住的是单测覆盖不到的一类问题：ChatMemory / ChatClient.Builder 是否存在、Mapper 扫描与
 * XML 是否能解析、向量库与模型自动配置会不会互相冲突、加锁工具与业务 Bean 的注入是否闭环。
 * 模型、向量库与分布式锁都用替身：测试不访问网络，也不需要 Redis / Qdrant / MySQL。
 *
 * @author wxy
 * @date 2026/10/05
 */
@SpringBootTest(
        classes = AiAgentApplication.class,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "feign.sentinel.enabled=false",
                "zza.lock.enabled=false",
                "zza.ai-agent.rag.enabled=false",
                "zza.ai-agent.knowledge.storage-type=local",
                "zza.ai-agent.knowledge.local-path=./target/test-knowledge",
                "spring.ai.dashscope.api-key=test-key",
                "spring.ai.dashscope.base-url=http://localhost:18080",
                "spring.ai.dashscope.chat.options.model=test-chat-model",
                "spring.ai.dashscope.embedding.options.model=test-embedding-model",
                "spring.ai.vectorstore.qdrant.initialize-schema=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:aiagent;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema.sql"
        })
class AiAgentApplicationTest {

    /** 对话模型替身：避免装配真实的 DashScope 模型 */
    @MockitoBean
    private ChatModel chatModel;

    /** 向量化模型替身 */
    @MockitoBean
    private EmbeddingModel embeddingModel;

    /** 向量库替身：避免连接 Qdrant */
    @MockitoBean
    private VectorStore vectorStore;

    /** 加锁工具替身：避免连接 Redis */
    @MockitoBean
    private DistributedLockUtil distributedLockUtil;

    /**
     * 上下文能正常启动
     */
    @Test
    @DisplayName("应用上下文：使用内存库与替身可正常装配")
    void contextLoads() {
        // 只要 Spring 能启动就算通过：Bean 缺失、自动配置冲突、Mapper XML 解析失败都会在这里暴露
    }
}
