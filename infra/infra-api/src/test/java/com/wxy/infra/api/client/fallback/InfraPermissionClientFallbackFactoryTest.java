package com.wxy.infra.api.client.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.dto.PermissionCheckReqDTO;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 权限客户端降级工厂单元测试：降级必须按「无权限」处理，不能放行。
 *
 * @author wxy
 * @date 2026/10/03
 */
class InfraPermissionClientFallbackFactoryTest {

    /**
     * 触发降级时返回 false（无权限）
     */
    @Test
    @DisplayName("create：降级时按无权限处理")
    void shouldReturnFalseWhenDegraded() {
        InfraPermissionClientFallbackFactory fallbackFactory = new InfraPermissionClientFallbackFactory();
        PermissionCheckReqDTO reqDTO = new PermissionCheckReqDTO();
        reqDTO.setUserId(1L);
        reqDTO.setPermissions(List.of("infra:user:create"));

        Result<Boolean> result = fallbackFactory.create(new TimeoutException("read timeout"))
                .hasAnyPermission(reqDTO);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isFalse();
    }
}
