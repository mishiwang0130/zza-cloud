package com.wxy.infra.biz.vo;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传分片的返回体：回显分片序号与对象存储给出的 ETag。
 *
 * <p>ETag 只用于排查与日志比对，合并时服务端会自己从对象存储读取分片清单，
 * 不依赖客户端提交的 ETag。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileChunkUploadRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 分片序号，从 1 开始 */
    private Integer partNumber;

    /** 分片 ETag，由对象存储返回 */
    private String etag;
}
