package com.wxy.rental.biz.vo.app;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户端房源图片返回体：App 公寓 / 房间详情里的图片项。
 *
 * <p>按契约稿与管理端的 {@code vo/admin/ImageRespVO} 各留一份：字段现在相同，但两端以后可能分叉（例如 App 要补宽高用于瀑布流），共用会让改动互相牵制。
 *
 * <p>{@code url} 是查询时按 {@code fileId} 向 infra 重新签发的预签名地址，因此不落库、不缓存。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ImageRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 图片关系记录 ID */
    private Long id;

    /** 文件 ID */
    private Long fileId;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 预签名访问地址：由 fileId 向 infra 文件接口换取 */
    private String url;
}
