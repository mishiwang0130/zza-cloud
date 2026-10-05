package com.wxy.common.lock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 分布式锁工具单元测试：不连真实 Redis，只验证「抢到与否的判定」与「释放的安全边界」。
 *
 * <p>重点守住两条：线程被中断时不能继续抢锁（要恢复中断标记）；释放锁只在当前线程持有时才执行
 * ——否则会把别的实例持有的锁删掉。
 *
 * @author wxy
 * @date 2026/10/05
 */
class DistributedLockUtilTest {

    /** 测试用锁 key */
    private static final String LOCK_KEY = "zza:ai-agent:chat:lock:1";

    /** Redisson 客户端替身 */
    private RedissonClient redissonClient;

    /** 锁替身 */
    private RLock lock;

    /** 被测工具 */
    private DistributedLockUtil distributedLockUtil;

    /**
     * 每个用例前重建替身
     */
    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        distributedLockUtil = new DistributedLockUtil(redissonClient);
    }

    /**
     * 抢到锁时返回 true
     *
     * @throws InterruptedException 替身方法声明的受检异常
     */
    @Test
    @DisplayName("tryLock：抢到锁返回 true")
    void tryLockShouldReturnTrueWhenAcquired() throws InterruptedException {
        when(lock.tryLock(eq(0L), eq(60_000L), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        assertThat(distributedLockUtil.tryLock(LOCK_KEY, 0L, 60_000L)).isTrue();
    }

    /**
     * 等待被中断时返回 false，并恢复中断标记
     *
     * @throws InterruptedException 替身方法声明的受检异常
     */
    @Test
    @DisplayName("tryLock：线程被中断返回 false 且恢复中断标记")
    void tryLockShouldReturnFalseWhenInterrupted() throws InterruptedException {
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenThrow(new InterruptedException("interrupted"));

        assertThat(distributedLockUtil.tryLock(LOCK_KEY, 10L, 60_000L)).isFalse();
        assertThat(Thread.interrupted()).as("必须恢复中断标记，供上层结束当前请求").isTrue();
    }

    /**
     * 当前线程持有时才真正释放
     */
    @Test
    @DisplayName("unlock：当前线程持有时才释放")
    void unlockShouldReleaseWhenHeldByCurrentThread() {
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        distributedLockUtil.unlock(LOCK_KEY);

        verify(lock).unlock();
    }

    /**
     * 未持有时不释放：避免误删别人的锁
     */
    @Test
    @DisplayName("unlock：未持有时不释放")
    void unlockShouldSkipWhenNotHeldByCurrentThread() {
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        distributedLockUtil.unlock(LOCK_KEY);

        verify(lock, never()).unlock();
    }

    /**
     * 释放失败只记日志，不影响业务返回
     */
    @Test
    @DisplayName("unlock：释放异常被吞掉")
    void unlockShouldSwallowException() {
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalStateException("redis down")).when(lock).unlock();

        assertThatCode(() -> distributedLockUtil.unlock(LOCK_KEY)).doesNotThrowAnyException();
    }
}
