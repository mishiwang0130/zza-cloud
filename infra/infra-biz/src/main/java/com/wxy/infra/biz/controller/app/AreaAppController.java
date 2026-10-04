package com.wxy.infra.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraAreaService;
import com.wxy.infra.biz.vo.app.AreaAppRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端行政区划接口：最终对外路径为 {@code /api/infra/app-api/area/...}。
 *
 * <p>整个类标 {@link PermitAll}：匿名浏览房源时要用它做城市 / 区县筛选，此时用户还没有登录态；
 * 省市区是标准只读数据，匿名放开也不涉及隐私。
 *
 * @author wxy
 * @date 2026/10/04
 */
@PermitAll
@Tag(name = "用户端 - 行政区划")
@RestController
@RequestMapping("/area")
public class AreaAppController {

    /** 行政区划服务 */
    @Resource
    private InfraAreaService infraAreaService;

    /**
     * 查询完整的省市区树
     *
     * @return 省级为根的三级树
     */
    @Operation(summary = "查询行政区划树", description = "一次返回省市区三级，前端可本地缓存")
    @GetMapping("/listTree")
    public Result<List<AreaAppRespVO>> listTree() {
        return Result.success(infraAreaService.listAppTree());
    }

    /**
     * 查询某一级下的子级区划
     *
     * @param parentId 上级区划 ID，不传表示取省级
     * @return 子级区划列表
     */
    @Operation(summary = "查询子级区划", description = "不传 parentId 返回省级；前端三级联动逐级加载用")
    @GetMapping("/listChildren")
    public Result<List<AreaAppRespVO>> listChildren(
            @Parameter(description = "上级区划 ID，0 表示省级") @RequestParam(required = false) Long parentId) {
        return Result.success(infraAreaService.listAppChildren(parentId));
    }
}
