package com.wxy.ai.agent.biz.config;

import com.wxy.ai.agent.biz.storage.KnowledgeFileStorage;
import com.wxy.ai.agent.biz.storage.LocalKnowledgeFileStorage;
import com.wxy.ai.agent.biz.storage.MinioKnowledgeFileStorage;
import com.wxy.common.storage.util.MinioUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库文件存储装配：按 {@code zza.ai-agent.knowledge.storage-type} 在 MinIO 与本机磁盘之间切换。
 *
 * <p>默认本机磁盘（不依赖任何中间件也能跑通上传与检索）；要落对象存储时把
 * {@code storage-type} 改成 {@code minio}，同时配好 {@code zza.minio.*}。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Configuration
public class KnowledgeStorageConfiguration {

    /**
     * MinIO 存储：common-storage 只在配置了 {@code zza.minio.endpoint} 时装配 MinioUtil，
     * 这里用 ObjectProvider 判断，缺失时给出明确提示而不是启动期 NPE。
     *
     * @param minioUtilProvider MinIO 工具提供者
     * @return MinIO 存储实现
     */
    @Bean
    @ConditionalOnProperty(prefix = "zza.ai-agent.knowledge", name = "storage-type", havingValue = "minio")
    public KnowledgeFileStorage minioKnowledgeFileStorage(ObjectProvider<MinioUtil> minioUtilProvider) {
        MinioUtil minioUtil = minioUtilProvider.getIfAvailable();
        if (minioUtil == null) {
            throw new IllegalStateException("knowledge.storage-type=minio 需要 MinIO 配置："
                    + "请配置 zza.minio.endpoint / access-key / secret-key / bucket");
        }
        return new MinioKnowledgeFileStorage(minioUtil);
    }

    /**
     * 本机磁盘存储：默认实现
     *
     * @param properties 业务配置
     * @return 本机存储实现
     */
    @Bean
    @ConditionalOnProperty(prefix = "zza.ai-agent.knowledge", name = "storage-type",
            havingValue = "local", matchIfMissing = true)
    public KnowledgeFileStorage localKnowledgeFileStorage(AiAgentProperties properties) {
        return new LocalKnowledgeFileStorage(properties.getKnowledge().getLocalPath());
    }
}
