package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 访问凭证表 {@code infra_token} 的实体：MySQL 是凭证状态的权威数据，Redis 只做校验缓存。
 *
 * <p>表里只存 token 的 SHA-256 摘要：摘要足以做查找与失效判断，又不会让数据库里出现可直接冒用的凭证。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_token")
public class InfraToken extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** access token 的 SHA-256 摘要（十六进制小写，64 位），唯一索引 {@code uk_infra_token_token_hash} */
    private String tokenHash;

    /** 凭证所属用户 ID */
    private Long userId;

    /** 登录端类型：1 管理后台、2 用户端，取值见 {@code UserTypeEnum}，校验时必须与请求所在端一致 */
    private Integer userType;

    /** 登录用户名：冗余存储，校验时不必回查用户表 */
    private String username;

    /** 凭证过期时间，索引 {@code idx_infra_token_expire_time} */
    private LocalDateTime expireTime;

    /** 状态：0 有效、1 已失效，取值见 {@code CommonStatusEnum}；登出或轮换后置为 1 */
    private Integer status;

    /** 登录 IP，仅用于审计排查 */
    private String loginIp;

    /** 同一登录会话的续期凭证摘要，登出时按它把续期凭证一并失效 */
    private String refreshTokenHash;
}
