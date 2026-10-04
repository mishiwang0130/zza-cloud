package com.wxy.rental.biz.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 编码串工具单元测试：编解码两端的去空去重行为。
 *
 * @author wxy
 * @date 2026/10/04
 */
class RentalCodeUtilTest {

    /**
     * 拆分时去掉空白项并去重，保持原顺序
     */
    @Test
    @DisplayName("split：去空、去重并保持顺序")
    void splitShouldTrimAndDistinct() {
        assertThat(RentalCodeUtil.split(" near_subway , brand_apartment ,near_subway, ")).containsExactly(
                "near_subway", "brand_apartment");
        assertThat(RentalCodeUtil.split(null)).isEmpty();
        assertThat(RentalCodeUtil.split("  ")).isEmpty();
    }

    /**
     * 拼接时忽略 null 与空串，并去重
     */
    @Test
    @DisplayName("join：忽略 null 与空串并去重")
    void joinShouldIgnoreBlankAndDistinct() {
        assertThat(RentalCodeUtil.join(Arrays.asList("a", null, " ", "b", "a"))).isEqualTo("a,b");
        assertThat(RentalCodeUtil.join(List.of())).isEmpty();
        assertThat(RentalCodeUtil.join(null)).isEmpty();
    }
}
