/**
 * Redis 公共能力：{@code RedisUtil}（读写）、{@code RedisKeyUtil}（拼 key）、
 * {@code RedisKeyConstant}（公共前缀常量）与序列化配置。
 *
 * <p>包名约定：{@code common-redis} 对应 {@code com.wxy.common.redis}。
 * key 一律「公共前缀 + 服务前缀 + 业务键」，除确实不需要过期的，都要设置过期时间。
 */
package com.wxy.common.redis;
