package com.wxy.rental.biz.vo.app;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * App 我的租约详情返回体：列表字段 + 合同文件地址 + 房间图片。
 *
 * <p>合同只存 {@code fileId}，这里按需换回预签名地址，用户点「查看合同」时能直接打开；图片同理，避免前端再调一次房间详情。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppLeaseRespVO extends AppLeaseItemRespVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 合同文件 ID，0 表示尚未上传 */
    private Long contractFileId;

    /** 合同文件预签名访问地址；未上传或文件查不到时为 null */
    private String contractFileUrl;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 房间图片（含预签名访问地址） */
    private List<ImageRespVO> images;
}
