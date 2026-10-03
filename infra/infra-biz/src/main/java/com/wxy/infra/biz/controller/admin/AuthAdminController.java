package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.constant.HeaderConstant;
import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraAdminAuthService;
import com.wxy.infra.biz.vo.admin.AuthLoginReqVO;
import com.wxy.infra.biz.vo.admin.AuthMenuRespVO;
import com.wxy.infra.biz.vo.admin.AuthRefreshReqVO;
import com.wxy.infra.biz.vo.admin.AuthTokenRespVO;
import com.wxy.infra.biz.vo.admin.AuthUpdatePasswordReqVO;
import com.wxy.infra.biz.vo.admin.AuthUserInfoRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台认证接口：登录、续期、登出、当前用户信息、导航菜单与自身密码维护。
 *
 * <p>类上的 {@code /auth} 会被 common-webmvc 按包名自动加上 {@code /admin-api} 前缀，
 * 最终对外路径为 {@code /admin-api/auth/...}；登录与续期用 {@link PermitAll} 标注为免登录，其余接口都需要凭证。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Tag(name = "管理后台 - 认证")
@RestController
@RequestMapping("/auth")
public class AuthAdminController {

    /** 管理后台认证服务 */
    @Resource
    private InfraAdminAuthService infraAdminAuthService;

    /**
     * 登录
     *
     * @param reqVO   登录入参
     * @param request 当前请求，用于记录登录 IP
     * @return 凭证返回体
     */
    @PermitAll
    @Operation(summary = "登录", description = "校验用户名密码，返回访问凭证与续期凭证")
    @PostMapping("/login")
    public Result<AuthTokenRespVO> login(@Validated @RequestBody AuthLoginReqVO reqVO, HttpServletRequest request) {
        return Result.success(infraAdminAuthService.login(reqVO, request.getRemoteAddr()));
    }

    /**
     * 续期
     *
     * @param reqVO 续期入参
     * @return 新的凭证返回体
     */
    @PermitAll
    @Operation(summary = "续期", description = "用续期凭证换取新的一对凭证，旧续期凭证立即失效")
    @PostMapping("/refresh")
    public Result<AuthTokenRespVO> refresh(@Validated @RequestBody AuthRefreshReqVO reqVO) {
        return Result.success(infraAdminAuthService.refresh(reqVO));
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
        infraAdminAuthService.logout(authorization);
        return Result.success();
    }

    /**
     * 查询当前登录用户信息
     *
     * @return 用户信息（含角色与权限标识）
     */
    @Operation(summary = "查询当前用户信息", description = "返回昵称、角色编码与权限标识，前端据此控制按钮显隐")
    @GetMapping("/getUserInfo")
    public Result<AuthUserInfoRespVO> getUserInfo() {
        return Result.success(infraAdminAuthService.getUserInfo());
    }

    /**
     * 查询当前登录用户的导航菜单
     *
     * @return 菜单树（只含目录与菜单）
     */
    @Operation(summary = "查询当前用户菜单", description = "返回导航菜单树，不含按钮权限")
    @GetMapping("/listMenus")
    public Result<List<AuthMenuRespVO>> listMenus() {
        return Result.success(infraAdminAuthService.listMenus());
    }

    /**
     * 修改当前登录用户密码
     *
     * @param reqVO 修改密码入参
     * @return 空响应
     */
    @Operation(summary = "修改密码", description = "校验原密码，成功后该用户所有凭证失效，需要重新登录")
    @PostMapping("/updatePassword")
    public Result<Void> updatePassword(@Validated @RequestBody AuthUpdatePasswordReqVO reqVO) {
        infraAdminAuthService.updatePassword(reqVO);
        return Result.success();
    }
}
