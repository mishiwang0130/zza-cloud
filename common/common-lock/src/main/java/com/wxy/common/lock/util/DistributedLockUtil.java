package com.wxy.common.lock.util;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 分布式锁工具：所有服务的加锁入口，避免各业务各写一套 try/finally 与异常处理。
 *
 * <p><b>用锁的姿势</b>：{@code tryLock} 返回 {@code true} 才拿到锁，业务结束后必须
 * 在 {@code finally} 里 {@code unlock}；{@code unlock} 只在「当前线程持有该锁」时才真正释放，
 * 所以即使业务线程被中断、或被误调用，也不会把别的实例持有的锁删掉。
 *
 * <p><b>租期（leaseTime）</b>：显式传租期时，Redisson 不做看门狗续期，到期自动释放——
 * 这是刻意的：文档重建、AI 问答这类操作最多几十秒，租期到点还没做完说明已经异常，
 * 比起让锁一直挂着导致整条链路不可用，宁可让下一次请求重试。
 *
 * <p><b>失败即快速返回</b>：{@code tryLock} 拿不到锁时立即返回 {@code false}，
 * 由调用方决定是「提示操作进行中」还是「稍后重试」，工具层不做无限等待。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
public class DistributedLockUtil {

    /** Redisson 客户端：锁对象由它按 key 创建，同一 key 在所有实例上是同一把分布式锁 */
    private final RedissonClient redissonClient;

    /**
     * 构造加锁工具
     *
     * @param redissonClient Redisson 客户端
     */
    public DistributedLockUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 尝试加锁：最多等待 waitMillis，拿到后持有 leaseMillis 自动释放
     *
     * @param key         锁 key，建议由各模块的 key 工具方法拼接，不要手写字符串
     * @param waitMillis  最长等待毫秒数，0 表示不等待、立即返回结果
     * @param leaseMillis 持锁租期毫秒数，到期自动释放，必须大于 0
     * @return 拿到锁返回 true；等待超时或线程被中断返回 false
     */
    public boolean tryLock(String key, long waitMillis, long leaseMillis) {
        RLock lock = redissonClient.getLock(key);
        try {
            return lock.tryLock(Math.max(waitMillis, 0L), leaseMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            // 中断意味着当前请求该结束：恢复中断标记，交回上层处理，而不是继续抢占锁
            Thread.currentThread().interrupt();
            log.warn("[tryLock][等待锁被中断] key={}", key);
            return false;
        }
    }

    /**
     * 释放锁：只有当前线程持有该锁时才释放
     *
     * <p>释放失败不往外抛：锁没拿到就不需要释放，锁已因租期到期自动释放也不该影响业务返回，
     * 这两种情况只记一条警告日志。
     *
     * @param key 锁 key
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (RuntimeException ex) {
            log.warn("[unlock][释放锁失败] key={}, error={}", key, ex.getMessage());
        }
    }
}
