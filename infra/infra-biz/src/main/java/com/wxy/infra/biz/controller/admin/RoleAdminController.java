package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.service.InfraRoleService;
import com.wxy.infra.biz.vo.admin.RoleCreateReqVO;
import com.wxy.infra.biz.vo.admin.RolePageReqVO;
import com.wxy.infra.biz.vo.admin.RoleRespVO;
import com.wxy.infra.biz.vo.admin.RoleSimpleRespVO;
import com.wxy.infra.biz.vo.admin.RoleUpdateReqVO;
import com.wxy.infra.biz.vo.admin.RoleUpdateStatusReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台角色管理接口：最终对外路径为 {@code /admin-api/role/...}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Tag(name = "管理后台 - 角色管理")
@RestController
@RequestMapping("/role")
public class RoleAdminController {

    /** 角色服务 */
    @Resource
    private InfraRoleService infraRoleService;

    /**
     * 新增角色
     *
     * @param reqVO 新增入参
     * @return 新角色 ID
     */
    @Operation(summary = "新增角色", description = "角色编码唯一，可同时勾选菜单与按钮权限")
    @RequiresPermission(InfraPermissionConstant.ROLE_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody RoleCreateReqVO reqVO) {
        return Result.success(infraRoleService.createRole(reqVO));
    }

    /**
     * 修改角色
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改角色", description = "菜单权限以提交的菜单 ID 列表为准全量覆盖")
    @RequiresPermission(InfraPermissionConstant.ROLE_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody RoleUpdateReqVO reqVO) {
        infraRoleService.updateRole(reqVO);
        return Result.success();
    }

    /**
     * 删除角色
     *
     * @param id 角色 ID
     * @return 空响应
     */
    @Operation(summary = "删除角色", description = "已分配给用户或超级管理员角色不允许删除")
    @RequiresPermission(InfraPermissionConstant.ROLE_DELETE)
    @PostMapping("/delete")
    public Result<Void> delete(@Parameter(description = "角色 ID") @RequestParam Long id) {
        infraRoleService.deleteRole(id);
        return Result.success();
    }

    /**
     * 查询角色详情
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    @Operation(summary = "查询角色详情", description = "含已分配的菜单 ID")
    @RequiresPermission(InfraPermissionConstant.ROLE_QUERY)
    @GetMapping("/getById")
    public Result<RoleRespVO> getById(@Parameter(description = "角色 ID") @RequestParam Long id) {
        return Result.success(infraRoleService.getRole(id));
    }

    /**
     * 分页查询角色
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询角色", description = "支持名称、编码与状态过滤")
    @RequiresPermission(InfraPermissionConstant.ROLE_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<RoleRespVO>> page(@Validated @RequestBody RolePageReqVO reqVO) {
        return Result.success(infraRoleService.pageRole(reqVO));
    }

    /**
     * 查询启用角色列表
     *
     * @return 角色精简列表
     */
    @Operation(summary = "查询角色列表", description = "只返回启用角色，用于用户编辑弹窗的角色下拉")
    @RequiresPermission(InfraPermissionConstant.ROLE_QUERY)
    @GetMapping("/list")
    public Result<List<RoleSimpleRespVO>> list() {
        return Result.success(infraRoleService.listRole());
    }

    /**
     * 修改角色状态
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改角色状态", description = "启用 / 停用；超级管理员角色不允许停用")
    @RequiresPermission(InfraPermissionConstant.ROLE_UPDATE_STATUS)
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@Validated @RequestBody RoleUpdateStatusReqVO reqVO) {
        infraRoleService.updateStatus(reqVO);
        return Result.success();
    }
}
