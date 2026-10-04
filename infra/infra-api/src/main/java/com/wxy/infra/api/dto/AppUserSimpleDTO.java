package com.wxy.infra.api.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端用户精简 DTO：跨服务按 ID 查用户时只给「ID + 昵称 + 手机号」。
 *
 * <p>只返回这三项是刻意的：调用方（例如 rental 的后台预约、租约列表）要的是能联系到人的信息，
 * 不需要头像、状态、审计字段这些维护端字段，少传一点就少一份对 infra 内部结构的耦合。
 *
 * <p>手机号在这里不做脱敏：这类接口是服务间接口，调用方是内部管理后台的展示需求，
 * 拿到完整号码才能联系用户；脱敏只发生在前端直接面向用户的返回体上。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUserSimpleDTO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户端用户 ID */
    private Long id;

    /** 昵称，注册时用手机号脱敏值兜底，用户可在个人中心修改 */
    private String nickname;

    /** 登录手机号 */
    private String mobile;
}
