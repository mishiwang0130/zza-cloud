package com.wxy.infra.biz.controller.app;

import com.wxy.common.core.result.Result;
import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.vo.FileChunkIdReqVO;
import com.wxy.infra.biz.vo.FileChunkInitReqVO;
import com.wxy.infra.biz.vo.FileChunkInitRespVO;
import com.wxy.infra.biz.vo.FileChunkUploadRespVO;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import com.wxy.infra.biz.vo.app.FileAppRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        return Result.success(infraFileService.initChunkUpload(reqVO, InfraFileSourceEnum.APP));
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
        return Result.success(infraFileService.uploadChunk(uploadId, partNumber, file, InfraFileSourceEnum.APP));
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
        return Result.success(infraFileService.completeChunkUpload(reqVO.getUploadId(), InfraFileSourceEnum.APP));
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
        infraFileService.abortChunkUpload(reqVO.getUploadId(), InfraFileSourceEnum.APP);
        return Result.success();
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
