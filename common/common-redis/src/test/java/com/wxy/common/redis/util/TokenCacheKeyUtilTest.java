package com.wxy.common.redis.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 平台凭证缓存 key 单元测试：key 结构是跨服务契约，写歪了就会出现「一边写、一边读不到」。
 *
 * @author wxy
 * @date 2026/10/03
 */
class TokenCacheKeyUtilTest {

    /**
     * key 形如 {@code zza:token:{摘要}}，且不带模块段（它不属于某一个服务）
     */
    @Test
    @DisplayName("accessTokenKey：key 为 zza:token:{摘要}")
    void accessTokenKeyShouldUsePlatformPrefix() {
        assertThat(TokenCacheKeyUtil.accessTokenKey("abc123")).isEqualTo("zza:token:abc123");
    }

    /**
     * 续期凭证的 key 与访问凭证分开，避免两类凭证互相覆盖
     */
    @Test
    @DisplayName("refreshTokenKey：key 为 zza:refresh:{摘要}")
    void refreshTokenKeyShouldUsePlatformPrefix() {
        assertThat(TokenCacheKeyUtil.refreshTokenKey("abc123")).isEqualTo("zza:refresh:abc123");
    }
}
