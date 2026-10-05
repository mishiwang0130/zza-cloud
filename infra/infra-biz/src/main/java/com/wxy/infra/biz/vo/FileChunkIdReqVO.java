package com.wxy.infra.biz.vo;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 只带分片上传会话 ID 的请求体：合并与取消分片上传都用它。
 *
 * <p>放在 {@code vo} 根下：admin 端与 app 端共用。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
public class FileChunkIdReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 分片上传会话 ID，初始化接口返回 */
    @NotBlank(message = "上传会话 ID 不能为空")
    private String uploadId;
}
