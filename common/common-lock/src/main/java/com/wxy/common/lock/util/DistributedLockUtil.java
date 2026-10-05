package com.wxy.common.lock.util;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.util.StringUtils;

/**
 * 分布式锁工具：所有服务的加锁入口，避免各业务各写一套 try/finally 与异常处理。
 *
 * <p><b>两套原语，按「加锁与释放是不是同一个线程」来选</b>：
 * <ul>
 *   <li>{@link #tryLock}/{@link #unlock}：Redisson 的 {@code RLock}，<b>绑定持有线程</b>，
 *       必须在同一个线程里加锁并释放，适合普通同步业务（例如管理端的文档重建）；</li>
 *   <li>{@link #tryAcquirePermit}/{@link #releasePermit}：{@code RPermitExpirableSemaphore}，
 *       许可不属于线程、每个许可自带租期，适合<b>跨线程释放</b>的场景（例如 WebFlux/Reactor：
 *       请求线程加锁，Reactor 线程在流结束时释放）。</li>
 * </ul>
 *
 * <p><b>为什么不能拿 RLock 干跨线程释放</b>（真实踩过的坑）：{@code RLock} 的持有者记录的是
 * 「客户端 UUID + 线程 ID」，在别的线程里 {@code isHeldByCurrentThread()} 恒为 false、
 * {@code unlock()} 会直接抛 {@code IllegalMonitorStateException}；如果代码里加了
 * 「不是当前线程持有就跳过」的判断，锁就会被静默漏放，一直占到租期结束——表现为用户回答完一条后，
 * 短时间内再问就提示「正在回答上一条」。
 *
 * <p><b>租期（leaseTime/leaseMillis）</b>：两套原语都显式传租期，Redisson 不做看门狗续期，到期自动释放——
 * 这是刻意的：文档重建、AI 问答这类操作最多几十秒，租期到点还没做完说明已经异常，
 * 比起让锁一直挂着导致整条链路不可用，宁可让下一次请求重试。
 *
 * <p><b>失败即快速返回</b>：拿不到时立即返回失败，
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

    /**
     * 尝试获取一个「可过期许可」，用作跨线程释放的互斥锁
     *
     * <p>与 {@link #tryLock} 的区别：许可的持有者不是某个线程，所以可以在任意线程
     * （包括 Reactor 的 {@code doFinally}）用 {@link #releasePermit} 释放；许可同时带租期，
     * 即使进程崩溃，Redis 里的许可也会自动过期，不会把资源永久锁死。
     *
     * <p>许可数只在 key 不存在时初始化，重复调用不会重置已有的许可（Redisson 内部用
     * 「key 不存在才写入」的 Lua 脚本保证），所以可以放心每次获取前都调一次。
     *
     * @param key         许可 key，建议由各模块的 key 工具方法拼接
     * @param waitMillis  最长等待毫秒数，0 表示不等待、立即返回结果
     * @param leaseMillis 许可租期毫秒数，到期自动失效，必须大于 0
     * @return 拿到许可返回许可 ID（释放时必须原样传回）；没拿到返回 null
     */
    public String tryAcquirePermit(String key, long waitMillis, long leaseMillis) {
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        try {
            semaphore.trySetPermits(1);
            return semaphore.tryAcquire(Math.max(waitMillis, 0L), leaseMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[tryAcquirePermit][等待许可被中断] key={}", key);
            return null;
        }
    }

    /**
     * 释放许可：可以在任意线程调用
     *
     * <p>用 {@code tryRelease} 而不是 {@code release}：许可已因租期到期被回收时，
     * {@code release} 会抛异常，而这里只需要记一条日志——业务已经跑完了，
     * 释放失败不该影响返回。传 null/空 ID 时直接忽略，避免调用方写多余的判空。
     *
     * @param key      许可 key
     * @param permitId {@link #tryAcquirePermit} 返回的许可 ID
     */
    public void releasePermit(String key, String permitId) {
        if (!StringUtils.hasText(permitId)) {
            return;
        }
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        try {
            if (!semaphore.tryRelease(permitId)) {
                log.warn("[releasePermit][许可已过期或已释放] key={}, permitId={}", key, permitId);
            }
        } catch (RuntimeException ex) {
            log.warn("[releasePermit][释放许可失败] key={}, error={}", key, ex.getMessage());
        }
    }
}
