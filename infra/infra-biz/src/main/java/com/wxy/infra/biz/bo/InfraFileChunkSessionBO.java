package com.wxy.infra.biz.bo;

import com.wxy.infra.biz.enums.InfraFileChunkStatusEnum;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 分片上传会话：一次分片上传从初始化到合并期间的全部状态，缓存在 Redis 里（值为 JSON）。
 *
 * <p>{@link #uploadId} 是本服务生成的、对外暴露的会话 ID；{@link #minioUploadId} 是对象存储给出的
 * 分片上传 ID，只在本服务内部使用。客户端只拿 uploadId，桶名与对象名都由会话决定，
 * 这样客户端无法指定任意对象名，避免把文件写到别人的目录里。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
public class InfraFileChunkSessionBO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 本服务生成的分片上传会话 ID，对客户端暴露 */
    private String uploadId;

    /** 对象存储给出的分片上传 ID，只在服务端内部使用 */
    private String minioUploadId;

    /** 对象名（key），初始化时生成，之后不再变化 */
    private String objectName;

    /** 上传时的原始文件名 */
    private String name;

    /** 文件总大小（字节），由客户端在初始化时声明 */
    private Long size;

    /** 内容类型（MIME） */
    private String contentType;

    /** 分片大小（字节），由服务端按配置决定 */
    private Long chunkSize;

    /** 总分片数，按文件大小与分片大小算出 */
    private Integer totalChunks;

    /** 上传来源端，取值见 {@code InfraFileSourceEnum} 的名字 */
    private String source;

    /** 上传用户 ID：分片接口按它校验会话归属，别人的 uploadId 传了也用不了 */
    private Long userId;

    /** 文件摘要（MD5），只用于续传时定位同一个文件，不做秒传去重 */
    private String fileMd5;

    /** 会话状态 */
    private InfraFileChunkStatusEnum status;

    /** 合并成功后落库的文件记录 ID，仅 {@link InfraFileChunkStatusEnum#COMPLETED} 时有值 */
    private Long fileId;
}
