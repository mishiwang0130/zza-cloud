package com.wxy.infra.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户端通用文件上传接口：目前用于上传头像等 app 端附件。
 *
 * <p>类上的 {@code /file} 会被 common-webmvc 按包名自动加上 {@code /app-api} 前缀，
 * 最终对外路径为 {@code /app-api/file/upload}。
 *
 * <p>只校验登录、不挂权限标识：app 用户不走管理后台的菜单按钮权限模型，
 * 上传是所有 app 页面都可能用到的基础能力。
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
}
