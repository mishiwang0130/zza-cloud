package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 续期凭证表 {@code infra_token_refresh} 的实体：access token 过期后凭它换取新的一对凭证。
 *
 * <p>续期凭证是不透明随机串（不是 JWT），是否有效完全以 MySQL 为准；
 * 每次续期都做轮换，旧的续期凭证立即失效，降低泄露后被反复利用的风险。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_token_refresh")
public class InfraTokenRefresh extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 续期凭证的 SHA-256 摘要（十六进制小写，64 位），唯一索引 {@code uk_infra_token_refresh_token_hash} */
    private String tokenHash;

    /** 凭证所属用户 ID */
    private Long userId;

    /** 登录端类型：1 管理后台、2 用户端，取值见 {@code UserTypeEnum} */
    private Integer userType;

    /** 登录用户名：冗余存储，续期时不必回查用户表（app 端用户表与本服务的用户表不同，也不能回查） */
    private String username;

    /** 凭证过期时间，索引 {@code idx_infra_token_refresh_expire_time} */
    private LocalDateTime expireTime;

    /** 状态：0 有效、1 已失效，取值见 {@code CommonStatusEnum} */
    private Integer status;

    /** 登录 IP，仅用于审计排查 */
    private String loginIp;
}
