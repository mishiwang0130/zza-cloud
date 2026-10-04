package com.wxy.infra.api.client;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.fallback.InfraAppUserClientFallbackFactory;
import com.wxy.infra.api.constant.InfraApiConstant;
import com.wxy.infra.api.dto.AppUserSimpleDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * infra 对外发布的用户端用户读取接口：其他服务只存了 {@code userId} 时，调它换回昵称与手机号。
 *
 * <p>只做「按 ID 批量查」，不提供分页与搜索：用户档案由 infra 自己维护，
 * 业务服务（例如 rental 的看房预约、租约）只需要把 ID 还原成可展示、可联系的人。
 *
 * <p><b>熔断降级</b>：调用失败或被熔断时返回失败响应，调用方 {@code requireData()} 抛异常。
 * 生效需要 {@code feign.sentinel.enabled: true}。
 *
 * @author wxy
 * @date 2026/10/05
 */
@FeignClient(name = InfraApiConstant.SERVICE_NAME, contextId = "infraAppUserClient",
        fallbackFactory = InfraAppUserClientFallbackFactory.class)
public interface InfraAppUserClient {

    /**
     * 按 ID 批量查询用户端用户
     *
     * @param ids 用户 ID 列表，为空时返回空列表
     * @return 用户列表，查不到的 ID 直接不返回
     */
    @GetMapping("/internal-api/app-user/listByIds")
    Result<List<AppUserSimpleDTO>> listByIds(@RequestParam("ids") List<Long> ids);
}
