package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.vo.admin.FileUploadRespVO;
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
 * 管理后台通用文件上传接口：只提供上传能力，不做文件列表与删除等管理功能。
 *
 * <p>只校验登录、不挂权限标识：上传是所有后台页面都可能用到的基础能力，不做成按按钮授权的粒度。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Tag(name = "管理后台 - 文件上传")
@RestController
@RequestMapping("/file")
public class FileAdminController {

    /** 文件服务 */
    @Resource
    private InfraFileService infraFileService;

    /**
     * 上传文件
     *
     * @param file 表单里的文件字段，字段名固定为 {@code file}
     * @return 对象名与预签名访问地址
     */
    @Operation(summary = "上传文件", description = "单个文件不超过 10MB，返回对象名与预签名访问地址")
    @PostMapping("/upload")
    public Result<FileUploadRespVO> upload(
            @Parameter(description = "上传的文件") @RequestPart("file") MultipartFile file) {
        return Result.success(infraFileService.upload(file));
    }
}
