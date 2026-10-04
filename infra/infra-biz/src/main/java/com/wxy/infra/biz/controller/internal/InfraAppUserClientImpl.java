package com.wxy.infra.biz.controller.internal;

import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraAppUserClient;
import com.wxy.infra.api.dto.AppUserSimpleDTO;
import com.wxy.infra.biz.convert.InfraAppUserConvert;
import com.wxy.infra.biz.service.InfraAppUserService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
 * infra 用户端用户读取服务间接口的实现：实现 infra-api 发布的 {@link InfraAppUserClient}。
 *
 * <p>只做「按 ID 批量查 + 剥离内部字段」，不提供按手机号搜索、不提供分页：
 * 用户档案的维护在 infra 自己手里，业务服务（rental）只需要把存的 {@code userId} 还原成人。
 *
 * <p>路径与参数绑定都在 client 上，本类只写实现；服务间接口不对外，因此不进接口文档（{@link Hidden}）。
 *
 * <p><b>它必须只在内网可达</b>：该前缀在 {@code zza.security.permit-all-urls} 里放行
 * （不做令牌校验，身份由调用方自己透传），外部访问由网关的 InternalEndpointBlockFilter
 * 按 {@code /internal-api} 前缀挡掉。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Hidden
@RestController
public class InfraAppUserClientImpl implements InfraAppUserClient {

    /** 用户端用户服务 */
    @Resource
    private InfraAppUserService infraAppUserService;

    /** 用户端用户转换器 */
    @Resource
    private InfraAppUserConvert infraAppUserConvert;

    /**
     * 按 ID 批量查询用户端用户
     *
     * @param ids 用户 ID 列表
     * @return 用户列表
     */
    @Override
    public Result<List<AppUserSimpleDTO>> listByIds(List<Long> ids) {
        return Result.success(infraAppUserConvert.toDTOList(infraAppUserService.listByIds(ids)));
    }
}
