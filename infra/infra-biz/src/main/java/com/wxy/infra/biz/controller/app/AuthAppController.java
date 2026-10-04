package com.wxy.infra.biz.controller.app;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraAppAuthService;
import com.wxy.infra.biz.vo.AuthTokenRespVO;
import com.wxy.infra.biz.vo.app.AppAuthUserInfoRespVO;
import com.wxy.infra.biz.vo.app.AuthAppLoginReqVO;
import com.wxy.infra.biz.vo.app.AuthAppRefreshReqVO;
import com.wxy.infra.biz.vo.app.AuthAppUpdateProfileReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端认证接口：最终对外路径为 {@code /api/infra/app-api/auth/...}。
 *
 * <p>登录与续期是匿名可访问的（此时还没有有效凭证），用 {@link PermitAll} 放开；
 * 登出、查询与修改个人资料要求登录，由公共凭证拦截器按 {@code /app-api} 前缀校验用户端身份。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "用户端 - 认证")
@RestController
@RequestMapping("/auth")
public class AuthAppController {
    @Resource
    private InfraAppAuthService infraAppAuthService;

    @PermitAll
    @Operation(summary = "登录", description = "校验用户手机号和验证码，返回访问凭证与续期凭证")
    @PostMapping("/login")
    public Result<AuthTokenRespVO> login(@Validated @RequestBody AuthAppLoginReqVO reqVO, HttpServletRequest request) {
        return Result.success(infraAppAuthService.login(reqVO, request.getRemoteAddr()));
    }

    /**
     * 续期
     *
     * @param reqVO 续期入参
     * @return 新的凭证返回体
     */
    @PermitAll
    @Operation(summary = "续期", description = "用用户端续期凭证换取新的一对凭证，旧续期凭证立即失效")
    @PostMapping("/refresh")
    public Result<AuthTokenRespVO> refresh(@Validated @RequestBody AuthAppRefreshReqVO reqVO) {
        return Result.success(infraAppAuthService.refresh(reqVO));
    }

    /**
     * 登出
     *
     * @param authorization 请求头里的凭证
     * @return 空响应
     */
    @Operation(summary = "登出", description = "当前凭证与同一会话的续期凭证立即失效")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = HeaderConstant.AUTHORIZATION, required = false) String authorization) {
        infraAppAuthService.logout(authorization);
        return Result.success();
    }

    /**
     * 查询当前登录用户信息
     *
     * @return 用户信息（手机号脱敏、头像含预签名地址）
     */
    @Operation(summary = "查询当前用户信息", description = "返回昵称与脱敏手机号，头像带预签名访问地址")
    @GetMapping("/getUserInfo")
    public Result<AppAuthUserInfoRespVO> getUserInfo() {
        return Result.success(infraAppAuthService.getUserInfo());
    }

    /**
     * 修改当前登录用户的个人资料
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改个人资料", description = "修改昵称与头像；头像传 0 表示清空")
    @PostMapping("/updateProfile")
    public Result<Void> updateProfile(@Validated @RequestBody AuthAppUpdateProfileReqVO reqVO) {
        infraAppAuthService.updateProfile(reqVO);
        return Result.success();
    }
}
