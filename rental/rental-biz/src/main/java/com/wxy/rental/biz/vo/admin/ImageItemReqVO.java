package com.wxy.rental.biz.vo.admin;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 房源图片提交项：公寓 / 房间新增修改请求里的一项。
 *
 * <p>只提交 {@code fileId} 与排序号，不提交访问地址：地址是预签名的、会过期，
 * 落库就是脏数据；详情返回时按 {@code fileId} 重新签发。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class ImageItemReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件 ID：由 infra 的文件上传接口返回 */
    @NotNull(message = "图片文件 ID 不能为空")
    private Long fileId;

    /** 排序号，越小越靠前；不传按 0 处理 */
    private Integer sort;
}
