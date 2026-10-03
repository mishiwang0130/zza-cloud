package com.wxy.infra.biz.bo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 凭证校验缓存对象：Redis 里缓存的 token 精简信息。
 *
 * <p>用普通 POJO 而不是 record：缓存值由 Fastjson2 反序列化，普通 POJO 的构造与字段绑定最稳妥。
 * 缓存里不放 token 摘要本身，避免缓存被导出后直接看到可用于定位凭证的键。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Data
public class InfraTokenCacheBO implements Serializable {

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
