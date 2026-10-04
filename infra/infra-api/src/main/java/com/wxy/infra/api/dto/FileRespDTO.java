package com.wxy.infra.api.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件 DTO：按 ID 查文件时返回元数据与预签名访问地址。
 *
 * <p>预签名地址是临时签出来的，不能缓存落库；调用方（例如 rental 把图片的 fileId 换成展示地址）
 * 每次展示前重新查一次即可，地址过期由下次查询自然刷新。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileRespDTO implements Serializable {

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
