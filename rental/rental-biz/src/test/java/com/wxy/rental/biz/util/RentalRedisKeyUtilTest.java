package com.wxy.rental.biz.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Redis key 拼接单元测试：key 结构是缓存与清理的共同约定，改结构会直接影响线上已存在的 key。
 *
 * @author wxy
 * @date 2026/10/04
 */
class RentalRedisKeyUtilTest {

    /**
     * 字典 key 形如 zza:rental:dict:{类型}
     */
    @Test
    @DisplayName("dictKey：拼接为 zza:rental:dict:{字典类型}")
    void dictKeyShouldFollowPrefixRule() {
        assertThat(RentalRedisKeyUtil.dictKey("rental_room_orientation"))
                .isEqualTo("zza:rental:dict:rental_room_orientation");
    }

    /**
     * 区划 key 形如 zza:rental:area:tree
     */
    @Test
    @DisplayName("areaTreeKey：拼接为 zza:rental:area:tree")
    void areaTreeKeyShouldFollowPrefixRule() {
        assertThat(RentalRedisKeyUtil.areaTreeKey()).isEqualTo("zza:rental:area:tree");
    }
}
