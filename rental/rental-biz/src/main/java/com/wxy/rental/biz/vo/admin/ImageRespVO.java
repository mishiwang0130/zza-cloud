package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 房源图片返回体：公寓 / 房间详情里的图片项。
 *
 * <p>{@code url} 是查询时按 {@code fileId} 向 infra 重新签发的预签名地址，
 * 因此不能缓存、也不落库；同一张图片隔一段时间再查，地址可能会变。
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
