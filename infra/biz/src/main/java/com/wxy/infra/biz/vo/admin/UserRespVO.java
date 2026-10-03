package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 管理后台用户返回体：不含密码字段，避免哈希被带出去。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class UserRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String mobile;

    /** 状态：0 启用、1 停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 已分配的角色 ID 列表：编辑弹窗回显用 */
    private List<Long> roleIds;
}
