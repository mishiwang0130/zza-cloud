/**
 * Redis 公共能力：所有模块共用的 {@code RedisUtil}（读写工具）与全局 key 前缀常量
 * {@code CommonRedisKeyConstant}。
 *
 * <p>包名约定：{@code common-redis} 对应 {@code com.wxy.common.redis}。
 * key 统一为 {@code zza:{模块}:{业务}:{标识}}；各模块的 key 常量与拼接方法各写一份，
 * 跨模块共享的只有 {@code RedisUtil} 与全局前缀；除确实不需要过期的，所有 key 都要设置过期时间。
 */
package com.wxy.common.redis;
