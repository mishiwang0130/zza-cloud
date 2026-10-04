package com.wxy.infra.biz.vo.app;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端文件返回体：按 ID 批量查时只给「文件 ID + 预签名访问地址」。
 *
 * <p>不返回文件名与对象名：访客端只需要能展示 / 下载的地址，对象名属于服务端内部信息。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAppRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件 ID */
    private Long id;

    /** 预签名访问地址：有效期内可直接访问，过期后重新查询获取 */
    private String url;
}
