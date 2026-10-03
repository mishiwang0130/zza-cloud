package com.wxy.infra.api.client.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.dto.TokenCheckReqDTO;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 凭证客户端降级工厂单元测试：降级必须「拒绝」，不能给出任何身份。
 *
 * @author wxy
 * @date 2026/10/03
 */
class InfraTokenClientFallbackFactoryTest {

    /**
     * 触发降级时返回「服务调用失败」的失败响应
     */
    @Test
    @DisplayName("create：降级时返回服务调用失败的失败响应")
    void shouldReturnErrorResultWhenDegraded() {
        InfraTokenClientFallbackFactory fallbackFactory = new InfraTokenClientFallbackFactory();
        TokenCheckReqDTO reqDTO = new TokenCheckReqDTO();
        reqDTO.setToken("token");

        Result<?> result = fallbackFactory.create(new TimeoutException("read timeout")).checkToken(reqDTO);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(CommonErrorConstant.REMOTE_CALL_ERROR.code());
        assertThat(result.getData()).isNull();
    }
}
