package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraAreaService;
import com.wxy.infra.biz.vo.admin.AreaRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台行政区划接口：最终对外路径为 {@code /admin-api/area/...}。
 *
 * <p>刻意不挂 {@code @RequiresPermission}：省市区是所有业务表单（地址、门店、客户资料）都要用的基础数据，
 * 挂权限码意味着每个角色都得单独配一条，成本远大于收益；这里只要求登录。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "管理后台 - 行政区划")
@RestController
@RequestMapping("/area")
public class AreaAdminController {

    /** 行政区划服务 */
    @Resource
    private InfraAreaService infraAreaService;

    /**
     * 查询某一级下的子级区划
     *
     * @param parentId 上级区划 ID，不传表示取省级
     * @return 子级区划列表
     */
    @Operation(summary = "查询子级区划", description = "不传 parentId 返回省级；前端三级联动逐级加载用")
    @GetMapping("/listChildren")
    public Result<List<AreaRespVO>> listChildren(
            @Parameter(description = "上级区划 ID，0 表示省级") @RequestParam(required = false) Long parentId) {
        return Result.success(infraAreaService.listChildren(parentId));
    }

    /**
     * 查询完整的省市区树
     *
     * @return 省级为根的树
     */
    @Operation(summary = "查询行政区划树", description = "一次返回省市县三级，前端可本地缓存")
    @GetMapping("/listTree")
    public Result<List<AreaRespVO>> listTree() {
        return Result.success(infraAreaService.listTree());
    }
}
