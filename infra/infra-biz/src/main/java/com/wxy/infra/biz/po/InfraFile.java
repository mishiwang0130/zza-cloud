package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 上传文件表 {@code infra_file} 的实体：记录每个上传文件的元数据。
 *
 * <p>不存访问地址：MinIO 的地址是预签名的、会过期，存下来就是脏数据；
 * 需要下载地址时按 {@link #path} 重新签发（见 {@code MinioUtil.presignedGetUrl}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_file")
public class InfraFile extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 上传时的原始文件名，仅用于展示与检索；存储层用的是随机对象名 */
    private String name;

    /** 对象存储中的对象名（key），唯一索引 {@code uk_infra_file_path}，取文件时用它重新签发地址 */
    private String path;

    /** 文件大小（字节） */
    private Long size;

    /** 内容类型（MIME），浏览器未带时为 null */
    private String contentType;
}
