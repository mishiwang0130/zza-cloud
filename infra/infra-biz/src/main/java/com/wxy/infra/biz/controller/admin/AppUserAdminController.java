package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.infra.biz.constant.InfraPermissionConstant;
import com.wxy.infra.biz.service.InfraAppUserService;
import com.wxy.infra.biz.vo.AppUserRespVO;
import com.wxy.infra.biz.vo.admin.AppUserPageReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 App 用户接口：最终对外路径为 {@code /api/infra/admin-api/app-user/...}。
 *
 * <p>只有分页查询：App 用户是自己注册的账号，后台不做新增、修改、删除，
 * 列表的意义是「查得到人」——给 App 用户管理页看档案，也给租约表单挑承租人用。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Tag(name = "管理后台 - App 用户")
@RestController
@RequestMapping("/app-user")
public class AppUserAdminController {

    /** App 用户服务 */
    @Resource
    private InfraAppUserService infraAppUserService;

    /**
     * 分页查询 App 用户
     *
     * @param reqVO 分页与过滤条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询 App 用户",
            description = "keyword 同时匹配昵称（前后模糊）与手机号（前缀模糊）；App 用户由用户自己注册，后台只读")
    @RequiresPermission(InfraPermissionConstant.APP_USER_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<AppUserRespVO>> page(@Validated @RequestBody AppUserPageReqVO reqVO) {
        return Result.success(infraAppUserService.pageAppUser(reqVO));
    }
}
