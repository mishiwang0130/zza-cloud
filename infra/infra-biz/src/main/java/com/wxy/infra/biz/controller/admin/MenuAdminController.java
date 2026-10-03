package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.service.InfraMenuService;
import com.wxy.infra.biz.vo.admin.MenuCreateReqVO;
import com.wxy.infra.biz.vo.admin.MenuRespVO;
import com.wxy.infra.biz.vo.admin.MenuUpdateReqVO;
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
 * 管理后台菜单管理接口：最终对外路径为 {@code /admin-api/menu/...}。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Tag(name = "管理后台 - 菜单管理")
@RestController
@RequestMapping("/menu")
public class MenuAdminController {

    /** 菜单服务 */
    @Resource
    private InfraMenuService infraMenuService;

    /**
     * 新增菜单
     *
     * @param reqVO 新增入参
     * @return 新菜单 ID
     */
    @Operation(summary = "新增菜单", description = "类型：1 目录、2 菜单、3 按钮；按钮需要填写权限标识")
    @RequiresPermission(InfraPermissionConstant.MENU_CREATE)
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody MenuCreateReqVO reqVO) {
        return Result.success(infraMenuService.createMenu(reqVO));
    }

    /**
     * 修改菜单
     *
     * @param reqVO 修改入参
     * @return 空响应
     */
    @Operation(summary = "修改菜单", description = "不允许把自己或自己的子孙设为上级菜单")
    @RequiresPermission(InfraPermissionConstant.MENU_UPDATE)
    @PostMapping("/update")
    public Result<Void> update(@Validated @RequestBody MenuUpdateReqVO reqVO) {
        infraMenuService.updateMenu(reqVO);
        return Result.success();
    }

    /**
     * 删除菜单
     *
     * @param id 菜单 ID
     * @return 空响应
     */
    @Operation(summary = "删除菜单", description = "有子菜单或被角色引用时不允许删除")
    @RequiresPermission(InfraPermissionConstant.MENU_DELETE)
    @PostMapping("/delete")
    public Result<Void> delete(@Parameter(description = "菜单 ID") @RequestParam Long id) {
        infraMenuService.deleteMenu(id);
        return Result.success();
    }

    /**
     * 查询菜单详情
     *
     * @param id 菜单 ID
     * @return 菜单详情
     */
    @Operation(summary = "查询菜单详情")
    @RequiresPermission(InfraPermissionConstant.MENU_QUERY)
    @GetMapping("/getById")
    public Result<MenuRespVO> getById(@Parameter(description = "菜单 ID") @RequestParam Long id) {
        return Result.success(infraMenuService.getMenu(id));
    }

    /**
     * 查询菜单树
     *
     * @return 菜单树（含按钮）
     */
    @Operation(summary = "查询菜单树", description = "返回全部菜单，含按钮权限节点")
    @RequiresPermission(InfraPermissionConstant.MENU_QUERY)
    @GetMapping("/list")
    public Result<List<MenuRespVO>> list() {
        return Result.success(infraMenuService.listMenuTree());
    }
}
