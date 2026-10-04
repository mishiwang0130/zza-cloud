package com.wxy.infra.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraAppAuthService;
import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.app.AuthAppLoginReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端 - 认证")
@RestController
@RequestMapping("/auth")
public class AuthAppController {
    @Resource
    private InfraAppAuthService infraAppAuthService;

    @PermitAll
    @Operation(summary = "登录", description = "校验用户名密码，返回访问凭证与续期凭证")
    @PostMapping("/login")
    public Result<AuthTokenRespVO> login(@Validated @RequestBody AuthAppLoginReqVO reqVO, HttpServletRequest request) {
        return Result.success(infraAppAuthService.login(reqVO, request.getRemoteAddr()));
    }
}
