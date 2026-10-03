package com.wxy.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 摘要工具单元测试：摘要值必须稳定，它是库表与 Redis 里查凭证的唯一依据。
 *
 * @author wxy
 * @date 2026/10/03
 */
class DigestUtilTest {

    /**
     * 结果与标准 SHA-256 一致：算错就查不到凭证，而且不会有任何报错
     */
    @Test
    @DisplayName("sha256Hex：结果与标准 SHA-256 一致，入参为 null 时返回 null")
    void sha256HexShouldMatchStandardDigest() {
        assertThat(DigestUtil.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(DigestUtil.sha256Hex(null)).isNull();
    }
}
