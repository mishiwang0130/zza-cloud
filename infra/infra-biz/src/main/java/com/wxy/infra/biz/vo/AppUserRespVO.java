package com.wxy.infra.biz.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户端用户返回体：服务间接口把 {@code userId} 还原成昵称与手机号时使用，后台列表也复用它。
 *
 * <p>只含 ID、昵称、手机号、状态、创建时间：调用方要的是能展示、能联系到人的信息，
 * 头像这类内部字段不在这里。服务间接口只取前三项组装 DTO，状态与注册时间只给后台列表用。
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

    /** 状态：0 启用、1 停用，取值见 {@code CommonStatusEnum}；停用后不允许登录 */
    private Integer status;

    /** 注册时间，即记录创建时间 */
    private LocalDateTime createTime;
}
