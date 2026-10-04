package com.wxy.infra.biz.vo.app;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户端修改个人资料请求：昵称与头像。
 *
 * <p>只能修改自己的记录：目标用户由登录上下文决定，不接受前端传用户 ID。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AuthAppUpdateProfileReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 昵称：去首尾空格后长度 1~64 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    /** 头像文件 ID：0 表示清空头像；为 null 表示本次不改动头像 */
    private Long avatarFileId;
}
