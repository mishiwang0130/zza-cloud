package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 文件返回体：按 ID 批量查询时返回元数据与预签名访问地址。
 *
 * <p>与上传返回体 {@link FileUploadRespVO} 的区别是「带文件 ID、按 ID 查」：
 * 调用方存的是 ID，回显时按 ID 换地址。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class FileRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件 ID */
    private Long id;

    /** 上传时的原始文件名，仅用于展示与检索 */
    private String name;

    /** 对象存储中的对象名（key） */
    private String path;

    /** 预签名访问地址：有效期内可直接访问，过期后重新查询获取 */
    private String url;
}
