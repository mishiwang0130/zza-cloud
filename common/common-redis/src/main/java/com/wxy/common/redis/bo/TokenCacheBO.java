package com.wxy.common.redis.bo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 平台凭证缓存值：Redis 里缓存的令牌精简信息。
 *
 * <p>它是跨服务契约的一部分：签发凭证的服务（infra）写、其他服务读，
 * 所以类型放在 common-redis 而不是某个业务模块里，双方共用同一个结构，字段增删能一起编译发现。
 *
 * <p>用普通 POJO 而不是 record：缓存值由 Fastjson2 反序列化，普通 POJO 的构造与字段绑定最稳妥。
 * 缓存里不放令牌摘要本身，避免缓存被导出后直接看到可用于定位凭证的键。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class TokenCacheBO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 凭证所属用户 ID */
    private Long userId;

    /** 登录端类型：1 管理后台、2 用户端 */
    private Integer userType;

    /** 登录用户名 */
    private String username;

    /** 凭证过期时间，命中缓存时用它计算剩余有效期 */
    private LocalDateTime expireTime;

    /** 登录 IP */
    private String loginIp;
}
