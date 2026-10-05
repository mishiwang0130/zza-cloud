/**
 * common-lock 公共能力包：基于 Redisson 的分布式锁。
 *
 * <p>只解决「多实例部署下同一资源的操作必须互斥」这一件事：{@code RedissonConfig} 按
 * {@code spring.data.redis.*} 装配 {@code RedissonClient}，{@code DistributedLockUtil}
 * 提供统一的可重入锁使用方式。业务代码不直接操作 Redisson，也不各自拼加锁重试逻辑。
 *
 * <p>锁只是「降低并发冲突」，不是数据正确性的唯一防线：真正的唯一性约束仍然落在数据库唯一键上。
 *
 * @author wxy
 * @date 2026/10/05
 */
package com.wxy.common.lock;
