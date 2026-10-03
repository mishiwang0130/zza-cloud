package com.wxy.infra.biz.controller.internal;

import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.enums.UserTypeEnum;
import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraTokenService;
import com.wxy.infra.biz.vo.internal.TokenCheckReqVO;
import com.wxy.infra.biz.vo.internal.TokenCheckRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务内部接口：把 infra 的凭证校验能力开放给其他服务（其他服务自己没有用户表，调这里鉴权）。
 *
 * <p>路径用 {@code /internal-api} 开头，且类不在 {@code controller.admin} / {@code controller.app} 包下，
 * 因此不会被自动加端前缀；它也不属于任何端，端类型比对由调用方按需传入 {@code userType}。
 *
 * <p><b>这个路径只能在内网访问</b>：已经在 {@code zza.security.permit-all-urls} 里放行（服务自己不做令牌校验），
 * 所以网关必须拒绝转发 {@code /api/infra/internal-api/**}，否则等于把校验接口暴露给公网。
 *
 * <p>令牌无效时返回 HTTP 401（沿用全局异常处理器的约定），调用方按未登录处理即可。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Tag(name = "服务内部 - 凭证校验")
@RestController
@RequestMapping("/internal-api/auth")
public class TokenInternalController {

    /** 凭证服务 */
    @Resource
    private InfraTokenService infraTokenService;

    /**
     * 校验访问令牌
     *
     * @param reqVO 校验入参
     * @return 身份三要素
     */
    @Operation(summary = "校验访问令牌", description = "供其他服务鉴权使用；令牌无效返回 401")
    @PostMapping("/check")
    public Result<TokenCheckRespVO> check(@Validated @RequestBody TokenCheckReqVO reqVO) {
        LoginUser loginUser = infraTokenService.validate(reqVO.getToken(), UserTypeEnum.of(reqVO.getUserType()));
        return Result.success(new TokenCheckRespVO(loginUser.userId(), loginUser.userType(), loginUser.username()));
    }
}
