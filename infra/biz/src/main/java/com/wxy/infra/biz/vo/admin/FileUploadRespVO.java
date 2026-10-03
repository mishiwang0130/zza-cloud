package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传返回体：对象名用于后续删除或复用，预签名地址用于直接访问。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 对象存储中的对象名（key） */
    private String objectName;

    /** 预签名访问地址：有效期由 {@code zza.minio.presigned-expiry-seconds} 决定 */
    private String url;
}
