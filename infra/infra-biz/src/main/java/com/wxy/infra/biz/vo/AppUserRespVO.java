package com.wxy.infra.biz.vo;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户端用户返回体：服务间接口把 {@code userId} 还原成昵称与手机号时使用。
 *
 * <p>只含 ID、昵称、手机号：调用方（例如 rental 后台列表）要的是能展示、能联系到人的信息，
 * 头像、状态、审计字段都不在服务间契约里。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
public class AppUserRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户端用户 ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 登录手机号 */
    private String mobile;
}
