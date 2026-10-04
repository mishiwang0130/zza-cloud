package com.wxy.infra.biz.vo;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传返回体：{@code fileId} 供业务表引用文件（头像、房源图片等都只存 ID），
 * 对象名用于复用与排查，预签名地址用于上传后立即回显。
 *
 * <p>放在 {@code vo} 包的根下而不是 {@code vo/admin}：admin 端与 app 端的上传接口返回体完全相同，
 * 它不属于任何一端，放进某一端的包会让另一端被迫依赖别人的包。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件记录 ID（{@code infra_file.id}）：业务表引用文件时只存它，展示地址按需重新签发 */
    private Long fileId;

    /** 对象存储中的对象名（key） */
    private String objectName;

    /** 预签名访问地址：有效期由 {@code zza.minio.presigned-expiry-seconds} 决定 */
    private String url;
}
