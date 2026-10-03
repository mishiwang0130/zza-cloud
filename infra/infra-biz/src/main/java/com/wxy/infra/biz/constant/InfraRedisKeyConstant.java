package com.wxy.infra.biz.constant;

import com.wxy.common.redis.constant.CommonRedisKeyConstant;

/**
 * infra 的 Redis key 前缀常量：全局前缀 + 本服务模块前缀。
 *
 * <p>key 统一为 {@code zza:infra:{业务}:{标识}}，具体拼接由 {@code InfraRedisKeyUtil} 提供，
 * 业务代码只引用常量与 Util，禁止硬编码字符串。
 *
 * <p>平台凭证缓存（访问凭证、续期凭证）不在这里：它是跨服务共享的缓存，
 * key 与缓存值定义在 common-redis（{@code CommonRedisKeyConstant.TOKEN} / {@code TokenCacheBO}），
 * 本类只维护 infra 自己的 key。
 *
 * @author wxy
 * @date 2026/10/03
 */
public final class InfraRedisKeyConstant {

    /** infra 模块前缀：全局前缀 + 服务名 */
    public static final String PREFIX = CommonRedisKeyConstant.PREFIX + "infra:";

    /** 用户权限集合缓存：admin 端为「用户-角色-菜单」算出的权限标识集合 */
    public static final String USER_PERMISSION = PREFIX + "perm:";

    /**
     * 工具类常量类，禁止实例化
     */
    private InfraRedisKeyConstant() {
    }
}
