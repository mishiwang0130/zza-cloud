package com.wxy.infra.biz.controller.admin;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.vo.FileChunkIdReqVO;
import com.wxy.infra.biz.vo.FileChunkInitReqVO;
import com.wxy.infra.biz.vo.FileChunkInitRespVO;
import com.wxy.infra.biz.vo.FileChunkUploadRespVO;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理后台通用文件上传接口：只提供上传能力，不做文件列表与删除等管理功能。
 *
 * <p>只校验登录、不挂权限标识：上传是所有后台页面都可能用到的基础能力，不做成按按钮授权的粒度。
 *
 * <p>与 app 端的上传接口（{@code /app-api/file/upload}）复用同一个 {@link InfraFileService}，
 * 这里传 {@link InfraFileSourceEnum#ADMIN}，对象名落在 {@code admin/} 目录下。
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
     * @return 文件记录 ID、对象名与预签名访问地址
     */
    @Operation(summary = "上传文件", description = "单个文件不超过 10MB，返回文件 ID、对象名与预签名访问地址")
    @PostMapping("/upload")
    public Result<FileUploadRespVO> upload(
            @Parameter(description = "上传的文件") @RequestPart("file") MultipartFile file) {
        return Result.success(infraFileService.upload(file, InfraFileSourceEnum.ADMIN));
    }

    /**
     * 初始化分片上传
     *
     * <p>文件超过 10MB 时走这里：先拿到 uploadId 与分片大小，再逐片上传，最后调合并接口。
     *
     * @param reqVO 文件名、大小、内容类型与 MD5
     * @return 会话 ID、分片大小、总分片数与已上传分片序号
     */
    @Operation(summary = "初始化分片上传",
            description = "文件超过 10MB 时先调它；返回 uploadId、分片大小与总分片数，续传时还会带回已上传的分片序号")
    @PostMapping("/fileChunk/init")
    public Result<FileChunkInitRespVO> initChunkUpload(@Validated @RequestBody FileChunkInitReqVO reqVO) {
        return Result.success(infraFileService.initChunkUpload(reqVO, InfraFileSourceEnum.ADMIN));
    }

    /**
     * 上传分片
     *
     * @param uploadId   初始化返回的会话 ID
     * @param partNumber 分片序号，从 1 开始
     * @param file       分片内容
     * @return 分片序号与 ETag
     */
    @Operation(summary = "上传分片", description = "按初始化返回的分片大小切片，分片序号从 1 开始；同一片重复上传会覆盖，可安全重试")
    @PostMapping(value = "/fileChunk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileChunkUploadRespVO> uploadChunk(
            @Parameter(description = "初始化返回的会话 ID") @RequestParam("uploadId") String uploadId,
            @Parameter(description = "分片序号，从 1 开始") @RequestParam("partNumber") Integer partNumber,
            @Parameter(description = "分片内容") @RequestPart("file") MultipartFile file) {
        return Result.success(infraFileService.uploadChunk(uploadId, partNumber, file, InfraFileSourceEnum.ADMIN));
    }

    /**
     * 合并分片
     *
     * @param reqVO 会话 ID
     * @return 文件记录 ID、对象名与预签名访问地址
     */
    @Operation(summary = "合并分片", description = "分片全部上传后调它合并成一个文件并落库；重复调用返回同一结果")
    @PostMapping("/fileChunk/complete")
    public Result<FileUploadRespVO> completeChunkUpload(@Validated @RequestBody FileChunkIdReqVO reqVO) {
        return Result.success(infraFileService.completeChunkUpload(reqVO.getUploadId(), InfraFileSourceEnum.ADMIN));
    }

    /**
     * 取消分片上传
     *
     * @param reqVO 会话 ID
     * @return 空响应
     */
    @Operation(summary = "取消分片上传", description = "用户放弃上传时调它清理已上传的分片，避免占用对象存储空间")
    @PostMapping("/fileChunk/abort")
    public Result<Void> abortChunkUpload(@Validated @RequestBody FileChunkIdReqVO reqVO) {
        infraFileService.abortChunkUpload(reqVO.getUploadId(), InfraFileSourceEnum.ADMIN);
        return Result.success();
    }
}
