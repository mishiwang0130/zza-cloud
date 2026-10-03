package com.wxy.infra.biz.service.impl;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.storage.util.MinioUtil;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.vo.admin.FileUploadRespVO;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务实现：把文件写入 MinIO，并返回预签名访问地址。
 *
 * <p>对象名按 {@code admin/{yyyyMMdd}/{uuid}{扩展名}} 生成：
 * 目录按天分隔便于排查与归档，随机名避免同名覆盖，也不把用户上传的原始文件名带到存储层
 * （原始文件名可能包含路径分隔符、控制字符等，直接当 key 有注入与可读性问题）。
 *
 * @author wxy
 * @date 2026/10/03
 */
@Slf4j
@Service
public class InfraFileServiceImpl implements InfraFileService {

    /** 单个文件大小上限（字节），与 {@code spring.servlet.multipart.max-file-size} 保持一致 */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /** 对象名里的日期目录格式 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);

    /** 保留的扩展名长度上限：超长的「扩展名」通常是伪造的，直接丢掉 */
    private static final int MAX_EXTENSION_LENGTH = 10;

    /**
     * 对象存储工具：common-storage 只在配置了 {@code zza.minio.endpoint} 时才会装配它。
     *
     * <p>用 {@link ObjectProvider} 而不是直接注入：没接对象存储的环境（例如只跑登录与用户管理）
     * 仍然能正常启动，调用上传接口时才报「文件上传失败」，而不是让整个服务起不来。
     */
    @Resource
    private ObjectProvider<MinioUtil> minioUtilProvider;

    /**
     * 上传文件到对象存储
     *
     * @param file 上传的文件
     * @return 对象名与预签名访问地址
     */
    @Override
    public FileUploadRespVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(InfraErrorConstant.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(InfraErrorConstant.FILE_SIZE_EXCEEDED,
                    "上传文件不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
        MinioUtil minioUtil = minioUtilProvider.getIfAvailable();
        if (minioUtil == null) {
            log.error("未配置对象存储，无法上传文件：请检查 zza.minio.endpoint");
            throw new BizException(InfraErrorConstant.FILE_UPLOAD_ERROR, "未配置对象存储");
        }
        String objectName = buildObjectName(file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            minioUtil.putObject(objectName, inputStream, file.getSize(), file.getContentType());
        } catch (IOException | RuntimeException ex) {
            // 存储不可用、桶不存在、鉴权失败等都在这里，统一转成业务错误，避免把内部细节泄露给调用方
            log.error("文件上传失败：objectName={}", objectName, ex);
            throw new BizException(InfraErrorConstant.FILE_UPLOAD_ERROR, null, ex);
        }
        return new FileUploadRespVO(objectName, minioUtil.presignedGetUrl(objectName));
    }

    /**
     * 生成对象名
     *
     * @param originalFilename 原始文件名，可以为空
     * @return 对象名
     */
    private String buildObjectName(String originalFilename) {
        return "admin/" + LocalDate.now().format(DATE_FORMATTER) + "/" + UUID.randomUUID() + resolveExtension(originalFilename);
    }

    /**
     * 提取扩展名，只保留短且为字母数字的情况
     *
     * @param originalFilename 原始文件名，可以为空
     * @return 形如 {@code .png} 的扩展名，无法识别时返回空串
     */
    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (extension.length() > MAX_EXTENSION_LENGTH || !extension.matches("[a-z0-9]+")) {
            return "";
        }
        return "." + extension;
    }
}
