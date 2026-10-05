package com.wxy.infra.biz.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 初始化分片上传的返回体：客户端按它切片与发送分片。
 *
 * <p>{@code chunkSize} 由服务端下发，客户端不要写死：对象存储对分片大小有下限要求，
 * 由服务端统一决定才能保证合并不报 {@code EntityTooSmall}。
 *
 * <p>放在 {@code vo} 根下：admin 端与 app 端返回体完全相同。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileChunkInitRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 分片上传会话 ID，后续上传分片、合并、取消都要带上它 */
    private String uploadId;

    /** 分片大小（字节），除最后一片外每片都按它切 */
    private Long chunkSize;

    /** 总分片数 */
    private Integer totalChunks;

    /** 已上传的分片序号（按升序），续传时客户端跳过它们即可 */
    private List<Integer> uploadedPartNumbers;
}
