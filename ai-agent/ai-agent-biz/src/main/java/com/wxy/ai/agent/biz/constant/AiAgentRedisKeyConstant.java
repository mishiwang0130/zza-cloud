package com.wxy.ai.agent.biz.constant;

import com.wxy.common.redis.constant.CommonRedisKeyConstant;

/**
 * ai-agent 的 Redis key 常量：全局前缀 + 本服务模块前缀。
 *
 * <p>只放「本服务自己拼的业务 key」。平台凭证缓存（{@code zza:token:*}）是跨服务共享的，
 * 由 common 定义；会话记忆（{@code spring.ai.memory.redis.key-prefix}）由框架的 Redis 会话记忆使用，
 * 这里只在配置里引用同一个前缀，不在代码里拼。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class AiAgentRedisKeyConstant {

    /** 本服务模块前缀：zza:ai-agent: */
    public static final String PREFIX = CommonRedisKeyConstant.PREFIX + "ai-agent:";

    /**
     * 同一会话并发提问的互斥许可前缀
     *
     * <p>用 {@code chat:turn:} 而不是最早的 {@code chat:lock:}：会话互斥从 Redisson 的 RLock
     * 换成了 {@code RPermitExpirableSemaphore}（RLock 绑定持有线程，无法在 Reactor 线程释放），
     * 两种数据结构在 Redis 里形态不同，换一个 key 段避免和旧版本可能残留的 key 撞在一起。
     */
    public static final String CHAT_LOCK = PREFIX + "chat:turn:";

    /** 同一知识文档重建 / 删除的互斥锁前缀 */
    public static final String KNOWLEDGE_LOCK = PREFIX + "knowledge:lock:";
    /**
     * 查看预约
     */
    public static final String VIEW_APPOINTMENT = PREFIX +"view:appointment:";

    /**
     * 工具类常量类，禁止实例化
     */
    private AiAgentRedisKeyConstant() {
    }
}
