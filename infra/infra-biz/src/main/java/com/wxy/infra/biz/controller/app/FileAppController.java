package com.wxy.infra.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import com.wxy.infra.biz.vo.app.FileAppRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户端文件接口：上传（头像等 app 端附件）与按 ID 批量换取访问地址。
 *
 * <p>类上的 {@code /file} 会被 common-webmvc 按包名自动加上 {@code /app-api} 前缀，
 * 最终对外路径为 {@code /app-api/file/upload}、{@code /app-api/file/listByIds}。
 *
 * <p>都不挂权限标识：app 用户不走管理后台的菜单按钮权限模型。上传只校验登录（所有 app 页面都可能用到）；
 * 查询地址额外标 {@link PermitAll}，因为匿名浏览房源时也要能把 {@code fileId} 换成可展示地址。
 *
 * <p>与后台的上传接口复用同一个 {@link InfraFileService}，这里传
 * {@link InfraFileSourceEnum#APP}，对象名落在 {@code app/} 目录下。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Tag(name = "用户端 - 文件上传")
@RestController
@RequestMapping("/file")
public class FileAppController {

    /** 文件服务 */
    @Resource
    private InfraFileService infraFileService;

    /**
     * 上传文件
     *
     * @param file 表单里的文件字段，字段名固定为 {@code file}
     * @return 文件记录 ID、对象名与预签名访问地址
     */
    @Operation(summary = "上传文件", description = "单个文件不超过 10MB，返回文件 ID、对象名与预签名访问地址")
    @PostMapping("/upload")
    public Result<FileUploadRespVO> upload(
            @Parameter(description = "上传的文件") @RequestPart("file") MultipartFile file) {
        return Result.success(infraFileService.upload(file, InfraFileSourceEnum.APP));
    }

    /**
     * 按 ID 批量查询文件访问地址
     *
     * <p>标 {@link PermitAll}：访客端匿名看房源时，拿到的图片只有 {@code fileId}，
     * 需要换成预签名地址才能展示，此时还没有登录态；地址是只读且有时效的，不涉及隐私。
     *
     * @param ids 文件 ID 列表，多个用英文逗号分隔
     * @return 文件列表（文件 ID + 预签名访问地址）；入参为空或查不到时返回空列表
     */
    @PermitAll
    @Operation(summary = "批量查询文件地址", description = "按 ID 批量换取预签名访问地址，入参为空或查不到时返回空列表")
    @GetMapping("/listByIds")
    public Result<List<FileAppRespVO>> listByIds(
            @Parameter(description = "文件 ID 列表，多个用英文逗号分隔") @RequestParam(required = false) List<Long> ids) {
        return Result.success(infraFileService.listAppByIds(ids));
    }
}
