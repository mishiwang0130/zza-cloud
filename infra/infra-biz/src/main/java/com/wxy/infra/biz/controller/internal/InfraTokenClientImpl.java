package com.wxy.infra.biz.controller.internal;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraTokenClient;
import com.wxy.infra.api.dto.TokenCheckReqDTO;
import com.wxy.infra.api.dto.TokenCheckRespDTO;
import com.wxy.infra.biz.service.InfraTokenService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RestController;

/**
 * infra 凭证服务间接口的实现：实现 infra-api 发布的 {@link InfraTokenClient}。
 *
 * <p>路径与参数绑定都取自接口（{@code @PostMapping}、{@code @Validated @RequestBody} 写在 client 里），
 * 本类只写业务实现——契约只有一份，改路径不会出现「实现忘了同步」的问题。
 *
 * <p>用 {@link Hidden} 而不是 {@code @Tag}/{@code @Operation}：服务间接口不对外，
 * 不需要出现在接口文档里。
 *
 * <p>类不在 {@code controller.admin} / {@code controller.app} 包下，不会被自动加端前缀。
 *
 * <p><b>它必须只在内网可达</b>：这套接口在 {@code zza.security.permit-all-urls} 里放行
 * （不做令牌校验，身份由调用方自己透传），外部访问由网关的 InternalEndpointBlockFilter
 * 按 {@code /internal-api} 前缀挡掉。
 *
 * <p>令牌无效时返回 HTTP 401（沿用全局异常处理器的约定），调用方按未登录处理即可。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Hidden
@RestController
public class InfraTokenClientImpl implements InfraTokenClient {

    /** 凭证服务 */
    @Resource
    private InfraTokenService infraTokenService;

    /**
     * 校验访问令牌
     *
     * @param reqDTO 校验入参
     * @return 身份三要素
     */
    @Override
    public Result<TokenCheckRespDTO> checkToken(TokenCheckReqDTO reqDTO) {
        LoginUser loginUser = infraTokenService.validate(reqDTO.getToken(), UserTypeEnum.of(reqDTO.getUserType()));
        return Result.success(new TokenCheckRespDTO(loginUser.userId(), loginUser.userType(), loginUser.username()));
    }
}
