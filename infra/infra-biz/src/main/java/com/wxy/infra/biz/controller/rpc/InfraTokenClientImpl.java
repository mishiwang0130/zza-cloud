package com.wxy.infra.biz.controller.rpc;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.result.Result;
import com.wxy.infra.api.client.InfraTokenClient;
import com.wxy.infra.api.constant.InfraApiConstant;
import com.wxy.infra.api.dto.TokenCheckReqDTO;
import com.wxy.infra.api.dto.TokenCheckRespDTO;
import com.wxy.infra.biz.service.InfraTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * infra 凭证服务间接口的实现：实现 infra-api 发布的 {@link InfraTokenClient}。
 *
 * <p>契约（路径、出入参）定义在 infra-api，本类只写实现；方法上重复声明一次映射，
 * 是为了不依赖「接口方法注解能否被 Spring MVC 继承」这一细节，路径统一取自 infra-api 的常量。
 *
 * <p>类不在 {@code controller.admin} / {@code controller.app} 包下，不会被自动加端前缀。
 *
 * <p><b>它必须只在内网可达</b>：这套接口在 {@code zza.security.permit-all-urls} 里放行
 * （不做令牌校验，身份由调用方自己透传），所以网关不能把 {@code /api/infra/rpc-api/**} 转发出去。
 *
 * <p>令牌无效时返回 HTTP 401（沿用全局异常处理器的约定），调用方按未登录处理即可。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Tag(name = "服务间接口 - 凭证校验")
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
    @Operation(summary = "校验访问令牌", description = "供其他服务鉴权使用；令牌无效返回 401")
    @Override
    @PostMapping(InfraApiConstant.TOKEN_API_PREFIX + InfraApiConstant.TOKEN_CHECK_PATH)
    public Result<TokenCheckRespDTO> checkToken(@Validated @RequestBody TokenCheckReqDTO reqDTO) {
        LoginUser loginUser = infraTokenService.validate(reqDTO.getToken(), UserTypeEnum.of(reqDTO.getUserType()));
        return Result.success(new TokenCheckRespDTO(loginUser.userId(), loginUser.userType(), loginUser.username()));
    }
}
