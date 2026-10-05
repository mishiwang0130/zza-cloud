package com.wxy.ai.agent.biz.storage;

import com.wxy.common.storage.util.MinioUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 MinIO 的知识库文件存储：复用 common-storage 的 {@code MinioUtil}。
 *
 * <p>桶名用 {@code zza.minio.bucket} 的默认桶，对象名由 Service 按
 * {@code {storage-prefix}{文档ID}/{文件名}} 拼好传进来。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
public class MinioKnowledgeFileStorage implements KnowledgeFileStorage {

    /** MinIO 操作工具 */
    private final MinioUtil minioUtil;

    /**
     * 构造存储实现
     *
     * @param minioUtil MinIO 操作工具
     */
    public MinioKnowledgeFileStorage(MinioUtil minioUtil) {
        this.minioUtil = minioUtil;
    }

    /**
     * 保存文件到对象存储
     *
     * @param objectName  对象名
     * @param content     文件内容
     * @param contentType 文件类型
     */
    @Override
    public void store(String objectName, byte[] content, String contentType) {
        minioUtil.putObject(objectName, new ByteArrayInputStream(content), content.length, contentType);
    }

    /**
     * 读取文件
     *
     * @param objectName 对象名
     * @return 文件流
     */
    @Override
    public InputStream open(String objectName) {
        return minioUtil.getObject(objectName);
    }

    /**
     * 删除文件
     *
     * @param objectName 对象名
     */
    @Override
    public void delete(String objectName) {
        minioUtil.removeObject(objectName);
    }
}
