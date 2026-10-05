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

    /** 短信验证码：值为验证码本身，过期时间由 {@code zza.sms.code-expire-minutes} 决定 */
    public static final String SMS_CODE = PREFIX + "sms:code:";

    /** 短信验证码发送间隔：只做占位去重，值不参与业务判断 */
    public static final String SMS_CODE_LIMIT = PREFIX + "sms:limit:";

    /** 分片上传会话：key 形如 {@code zza:infra:file:chunk:session:{uploadId}}，值为 {@code InfraFileChunkSessionBO} */
    public static final String FILE_CHUNK_SESSION = PREFIX + "file:chunk:session:";

    /**
     * 分片上传续传索引：key 形如 {@code zza:infra:file:chunk:resume:{userId}:{端}:{md5}:{大小}}，值为 uploadId。
     *
     * <p>用「用户 + 端 + 文件摘要 + 文件大小」定位同一个文件的上传会话，页面刷新或断网重连后
     * 重新初始化时会命中同一条会话，从而拿到已上传的分片序号继续传。
     */
    public static final String FILE_CHUNK_RESUME = PREFIX + "file:chunk:resume:";

    /**
     * 工具类常量类，禁止实例化
     */
    private InfraRedisKeyConstant() {
    }
}
