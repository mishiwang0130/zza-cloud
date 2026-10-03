package com.wxy.infra.biz.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 凭证工具单元测试：摘要、续期凭证生成与 Bearer 前缀剥离。
 *
 * @author wxy
 * @date 2026/10/03
 */
class InfraTokenUtilTest {

    /**
     * 生成的续期凭证必须是长度足够且不重复的随机串
     */
    @Test
    @DisplayName("generateRefreshToken：生成 32 字节随机串，两次结果不重复")
    void generateRefreshTokenShouldBeRandom() {
        String first = InfraTokenUtil.generateRefreshToken();
        String second = InfraTokenUtil.generateRefreshToken();
        assertThat(first).isNotBlank().isNotEqualTo(second);
        // 32 字节 Base64 URL 编码（无填充）固定为 43 个字符
        assertThat(first).hasSize(43);
    }

    /**
     * Bearer 前缀剥离要同时兼容标准头与裸 token
     */
    @Test
    @DisplayName("stripBearer：兼容 Bearer 前缀、裸 token 与空值")
    void stripBearerShouldSupportBothFormats() {
        assertThat(InfraTokenUtil.stripBearer("Bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(InfraTokenUtil.stripBearer("  abc.def.ghi  ")).isEqualTo("abc.def.ghi");
        assertThat(InfraTokenUtil.stripBearer(null)).isNull();
        assertThat(InfraTokenUtil.stripBearer("   ")).isNull();
    }
}
