package com.wxy.infra.biz.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 初始化分片上传的请求体。
 *
 * <p>{@code fileSize} 由客户端声明：服务端据此算出总分片数，合并前还会与对象存储里
 * 实际收到的分片总大小核对，声明错了合并不了。{@code fileMd5} 只用于续传定位同一文件，
 * 不做秒传复用。
 *
 * <p>放在 {@code vo} 根下而不是 {@code vo/admin}：admin 端与 app 端的分片接口请求体完全相同。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
public class FileChunkInitReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 原始文件名，只用于展示与落库，不参与对象名生成 */
    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过 255 个字符")
    private String fileName;

    /** 文件总大小（字节），必须大于 0 */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于 0")
    private Long fileSize;

    /** 内容类型（MIME），浏览器未带时可以为空 */
    @Size(max = 128, message = "内容类型长度不能超过 128 个字符")
    private String contentType;

    /** 文件摘要（MD5）：续传时用于定位同一个文件 */
    @NotBlank(message = "文件摘要不能为空")
    @Size(max = 64, message = "文件摘要长度不能超过 64 个字符")
    private String fileMd5;
}
