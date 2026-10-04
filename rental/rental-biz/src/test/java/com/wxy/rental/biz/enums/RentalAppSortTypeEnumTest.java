package com.wxy.rental.biz.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * App 房源列表排序枚举单元测试：认识的值原样返回，null 与非法值统一收敛为综合排序。
 *
 * @author wxy
 * @date 2026/10/04
 */
class RentalAppSortTypeEnumTest {

    /**
     * 认识的排序值原样返回
     */
    @Test
    @DisplayName("normalize：认识的值原样返回")
    void normalizeShouldKeepKnownValue() {
        assertThat(RentalAppSortTypeEnum.normalize(0)).isZero();
        assertThat(RentalAppSortTypeEnum.normalize(1)).isEqualTo(1);
        assertThat(RentalAppSortTypeEnum.normalize(2)).isEqualTo(2);
        assertThat(RentalAppSortTypeEnum.normalize(3)).isEqualTo(3);
    }

    /**
     * null 与非法排序值都按综合排序（0）处理
     */
    @Test
    @DisplayName("normalize：null 与非法值按综合排序")
    void normalizeShouldFallbackToComprehensive() {
        assertThat(RentalAppSortTypeEnum.normalize(null)).isZero();
        assertThat(RentalAppSortTypeEnum.normalize(99)).isZero();
        assertThat(RentalAppSortTypeEnum.normalize(-1)).isZero();
    }
}
