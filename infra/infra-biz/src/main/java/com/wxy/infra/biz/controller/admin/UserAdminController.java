package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.annotation.RequiresPermission;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.service.InfraUserService;
import com.wxy.infra.biz.vo.admin.UserCreateReqVO;
import com.wxy.infra.biz.vo.admin.UserPageReqVO;
import com.wxy.infra.biz.vo.admin.UserResetPasswordReqVO;
import com.wxy.infra.biz.vo.admin.UserRespVO;
import com.wxy.infra.biz.vo.admin.UserUpdateReqVO;
import com.wxy.infra.biz.vo.admin.UserUpdateStatusReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台用户管理接口：方法上的权限标识与 {@code infra_menu.perms} 里的按钮权限一一对应。
 *
 * <p>路径遵循「资源 + 动作」的写法，最终对外路径为 {@code /admin-api/user/...}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Tag(name = "管理后台 - 用户管理")
@RestController
@RequestMapping("/user")
public class UserAdminController {

    /** 用户服务 */
    @Resource
    private InfraUserService infraUserService;

    /**
     * 新增用户
     *
     * @param reqVO 新增入参
     * @return 新用户 ID
     */
    @Operation(summary = "新增用户", description = "用户名与手机号唯一，可同时分配角色")
    @RequiresPermission(InfraPermissionConstant.USER_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody UserCreateReqVO reqVO) {
        return Result.success(infraUserService.createUser(reqVO));
    }

    /**
     * 修改用户
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改用户", description = "修改昵称、手机号、状态与角色；用户名不可修改")
    @RequiresPermission(InfraPermissionConstant.USER_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody UserUpdateReqVO reqVO) {
        infraUserService.updateUser(reqVO);
        return Result.success();
    }

    /**
     * 删除用户
     *
     * @param id 用户 ID
     * @return 空响应
     */
    @Operation(summary = "删除用户", description = "逻辑删除；不能删除自己与超级管理员")
    @RequiresPermission(InfraPermissionConstant.USER_DELETE)
    @PostMapping("/delete")
    public Result<Void> delete(@Parameter(description = "用户 ID") @RequestParam Long id) {
        infraUserService.deleteUser(id);
        return Result.success();
    }

    /**
     * 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @Operation(summary = "查询用户详情", description = "含已分配的角色 ID，用于编辑弹窗回显")
    @RequiresPermission(InfraPermissionConstant.USER_QUERY)
    @GetMapping("/getById")
    public Result<UserRespVO> getById(@Parameter(description = "用户 ID") @RequestParam Long id) {
        return Result.success(infraUserService.getUser(id));
    }

    /**
     * 分页查询用户
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询用户", description = "支持用户名、昵称、手机号与状态过滤")
    @RequiresPermission(InfraPermissionConstant.USER_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<UserRespVO>> page(@Validated @RequestBody UserPageReqVO reqVO) {
        return Result.success(infraUserService.pageUser(reqVO));
    }

    /**
     * 修改用户状态
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改用户状态", description = "启用 / 停用；不能停用自己与超级管理员")
    @RequiresPermission(InfraPermissionConstant.USER_UPDATE_STATUS)
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@Validated @RequestBody UserUpdateStatusReqVO reqVO) {
        infraUserService.updateStatus(reqVO);
        return Result.success();
    }

    /**
     * 重置用户密码
     *
     * @param reqVO 重置入参
     * @return 空响应
     */
    @Operation(summary = "重置用户密码", description = "管理员直接设置新密码；超级管理员账号不允许重置")
    @RequiresPermission(InfraPermissionConstant.USER_RESET_PASSWORD)
    @PostMapping("/resetPassword")
    public Result<Void> resetPassword(@Validated @RequestBody UserResetPasswordReqVO reqVO) {
        infraUserService.resetPassword(reqVO);
        return Result.success();
    }
}
