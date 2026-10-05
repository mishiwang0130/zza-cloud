package com.wxy.ai.agent.biz.storage;

import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.common.core.exception.BizException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于本机磁盘的知识库文件存储：没有 MinIO 时用于本地调试。
 *
 * <p>存储根目录来自 {@code zza.ai-agent.knowledge.local-path}，对象名直接映射成相对路径。
 * 生产环境应使用 MinIO：本机磁盘在多实例部署下不共享，重建索引会读到别的实例的目录。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
public class LocalKnowledgeFileStorage implements KnowledgeFileStorage {

    /** 存储根目录 */
    private final Path root;

    /**
     * 构造存储实现
     *
     * @param localPath 存储根目录
     */
    public LocalKnowledgeFileStorage(String localPath) {
        this.root = Paths.get(localPath).toAbsolutePath().normalize();
        log.warn("知识库原始文件存本机磁盘 {}：多实例部署下请改用 MinIO", this.root);
    }

    /**
     * 保存文件：目录不存在时自动创建
     *
     * @param objectName  对象名
     * @param content     文件内容
     * @param contentType 文件类型（本机存储用不到，忽略）
     */
    @Override
    public void store(String objectName, byte[] content, String contentType) {
        Path target = resolve(objectName);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_INDEX_FAILED, "原始文件保存失败", ex);
        }
    }

    /**
     * 读取文件
     *
     * @param objectName 对象名
     * @return 文件流
     */
    @Override
    public InputStream open(String objectName) {
        try {
            return Files.newInputStream(resolve(objectName));
        } catch (IOException ex) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_INDEX_FAILED, "原始文件读取失败", ex);
        }
    }

    /**
     * 删除文件：文件不存在时忽略
     *
     * @param objectName 对象名
     */
    @Override
    public void delete(String objectName) {
        try {
            Files.deleteIfExists(resolve(objectName));
        } catch (IOException ex) {
            log.warn("删除本机知识库文件失败：objectName={}, error={}", objectName, ex.getMessage());
        }
    }

    /**
     * 对象名转绝对路径，并阻止 {@code ../} 越出存储根目录
     *
     * @param objectName 对象名
     * @return 绝对路径
     */
    private Path resolve(String objectName) {
        Path target = root.resolve(objectName).normalize();
        if (!target.startsWith(root)) {
            throw new BizException(AiAgentErrorConstant.KNOWLEDGE_INDEX_FAILED, "非法的文件路径");
        }
        return target;
    }
}
