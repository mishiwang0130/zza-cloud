package com.wxy.infra.biz.vo.app;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户端当前登录用户信息：个人中心展示用。
 *
 * <p>手机号返回脱敏值（如 138****0000）；头像只回文件 ID 与按需签发的访问地址，
 * 不把文件元数据整块带出去。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AppAuthUserInfoRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long id;

    /** 手机号（脱敏值，如 138****0000） */
    private String mobile;

    /** 昵称 */
    private String nickname;

    /** 头像文件 ID，0 表示未设置 */
    private Long avatarFileId;

    /** 头像预签名访问地址；未设置或文件查不到时为 null */
    private String avatarUrl;
}
