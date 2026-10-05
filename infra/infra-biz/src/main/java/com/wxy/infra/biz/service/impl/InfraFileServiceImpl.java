package com.wxy.infra.biz.service.impl;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.core.result.ErrorCode;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.common.storage.util.MinioUtil;
import com.wxy.infra.biz.bo.InfraFileChunkSessionBO;
import com.wxy.infra.biz.config.InfraFileProperties;
import com.wxy.infra.biz.constant.InfraErrorConstant;
import com.wxy.infra.biz.convert.InfraFileConvert;
import com.wxy.infra.biz.enums.InfraFileChunkStatusEnum;
import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.mapper.InfraFileMapper;
import com.wxy.infra.biz.po.InfraFile;
import com.wxy.infra.biz.service.InfraFileService;
import com.wxy.infra.biz.util.InfraRedisKeyUtil;
import com.wxy.infra.biz.vo.FileChunkInitReqVO;
import com.wxy.infra.biz.vo.FileChunkInitRespVO;
import com.wxy.infra.biz.vo.FileChunkUploadRespVO;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import com.wxy.infra.biz.vo.FileRespVO;
import com.wxy.infra.biz.vo.app.FileAppRespVO;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务实现：把文件写入 MinIO、落一条文件记录，并返回预签名访问地址。
 *
 * <p>对象名按 {@code {端}/{yyyyMMdd}/{uuid}{扩展名}} 生成：
 * 端前缀（admin / app）便于区分文件来源，目录按天分隔便于排查与归档，
 * 随机名避免同名覆盖，也不把用户上传的原始文件名带到存储层
 * （原始文件名可能包含路径分隔符、控制字符等，直接当 key 有注入与可读性问题）。
 *
 * <p>「存储 + 记录」是两步操作，没有共同的事务：入库失败时把刚上传的对象删掉（补偿），
 * 避免留下查不到宿主的孤儿文件；反过来对象存储失败时不会写记录。
 *
 * <p>按 ID 批量查询（服务间接口用）不缓存地址：预签名地址本身带过期时间，
 * 缓存下来等于把过期时间也缓存了，调用方可能拿到已经失效的链接。
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

    /** 原始文件名入库的长度上限，与 {@code infra_file.name} 保持一致 */
    private static final int MAX_NAME_LENGTH = 255;

    /** 内容类型入库的长度上限，与 {@code infra_file.content_type} 保持一致 */
    private static final int MAX_CONTENT_TYPE_LENGTH = 128;

    /** 分片大小下限：对象存储要求除最后一片外每片都不小于 5MiB */
    private static final long MIN_PART_SIZE = 5L * 1024 * 1024;

    /** 分片数上限：对象存储单次分片上传最多 10000 片 */
    private static final int MAX_PART_COUNT = 10_000;

    /** 合并完成的会话保留时间（秒）：供客户端重试 complete 时幂等返回同一结果 */
    private static final long COMPLETED_SESSION_KEEP_SECONDS = 3600L;

    /** 抢占续传索引失败后的重试次数：并发初始化时用赢家的会话，重试一次足够 */
    private static final int INIT_RESUME_RETRY_TIMES = 2;

    /**
     * 对象存储工具：common-storage 只在配置了 {@code zza.minio.endpoint} 时才会装配它。
     *
     * <p>用 {@link ObjectProvider} 而不是直接注入：没接对象存储的环境（例如只跑登录与用户管理）
     * 仍然能正常启动，调用上传接口时才报「文件上传失败」，而不是让整个服务起不来。
     */
    @Resource
    private ObjectProvider<MinioUtil> minioUtilProvider;

    /** 文件记录 Mapper */
    @Resource
    private InfraFileMapper infraFileMapper;

    /** 文件转换器：用户端返回体只保留文件 ID 与访问地址 */
    @Resource
    private InfraFileConvert infraFileConvert;

    /** 文件上传配置：分片阈值、分片大小、单文件上限与会话有效期 */
    @Resource
    private InfraFileProperties infraFileProperties;

    /** Redis 工具：保存分片上传会话与续传索引 */
    @Resource
    private RedisUtil redisUtil;

    /**
     * 上传文件到对象存储
     *
     * @param file   上传的文件
     * @param source 上传来源端，决定对象名的目录前缀
     * @return 文件记录 ID、对象名与预签名访问地址
     */
    @Override
    public FileUploadRespVO upload(MultipartFile file, InfraFileSourceEnum source) {
        if (file == null || file.isEmpty()) {
            throw new BizException(InfraErrorConstant.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(InfraErrorConstant.FILE_SIZE_EXCEEDED,
                    "上传文件不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
        MinioUtil minioUtil = requireMinioUtil(InfraErrorConstant.FILE_UPLOAD_ERROR,
                "未配置对象存储，无法上传文件：请检查 zza.minio.endpoint");
        String objectName = buildObjectName(source, file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            minioUtil.putObject(objectName, inputStream, file.getSize(), file.getContentType());
        } catch (IOException | RuntimeException ex) {
            // 存储不可用、桶不存在、鉴权失败等都在这里，统一转成业务错误，避免把内部细节泄露给调用方
            log.error("文件上传失败：objectName={}", objectName, ex);
            throw new BizException(InfraErrorConstant.FILE_UPLOAD_ERROR, null, ex);
        }
        InfraFile saved = saveFileRecord(file.getOriginalFilename(), objectName, file.getSize(),
                file.getContentType(), minioUtil);
        return new FileUploadRespVO(saved.getId(), objectName, minioUtil.presignedGetUrl(objectName));
    }

    /**
     * 初始化分片上传会话
     *
     * <p>先用「用户 + 端 + 文件摘要 + 大小」抢续传索引，抢到才创建对象存储的分片上传：
     * 这样同一个文件并发初始化只会产生一个会话，另一个请求直接复用，不会留下多份半成品分片。
     *
     * @param reqVO  初始化入参
     * @param source 上传来源端
     * @return 会话 ID、分片大小、总分片数与已上传分片序号
     */
    @Override
    public FileChunkInitRespVO initChunkUpload(FileChunkInitReqVO reqVO, InfraFileSourceEnum source) {
        Long userId = requireLoginUserId();
        long fileSize = reqVO.getFileSize();
        long maxFileSize = infraFileProperties.getMaxFileSize().toBytes();
        if (fileSize > maxFileSize) {
            throw new BizException(InfraErrorConstant.FILE_SIZE_EXCEEDED,
                    "上传文件不能超过 " + toMegabytes(maxFileSize) + "MB");
        }
        long chunkSize = resolveChunkSize();
        long totalChunks = (fileSize + chunkSize - 1) / chunkSize;
        if (totalChunks > MAX_PART_COUNT) {
            throw new BizException(InfraErrorConstant.FILE_SIZE_EXCEEDED,
                    "上传文件不能超过 " + toMegabytes(maxFileSize) + "MB");
        }
        MinioUtil minioUtil = requireMinioUtil(InfraErrorConstant.FILE_CHUNK_UPLOAD_ERROR,
                "未配置对象存储，无法分片上传：请检查 zza.minio.endpoint");
        String resumeKey = InfraRedisKeyUtil.fileChunkResumeKey(userId, source, reqVO.getFileMd5(), fileSize);
        long ttlSeconds = infraFileProperties.getSessionExpireSeconds();
        for (int attempt = 0; attempt < INIT_RESUME_RETRY_TIMES; attempt++) {
            InfraFileChunkSessionBO exists = findResumableSession(resumeKey, userId, source);
            if (exists != null) {
                return resumeChunkUpload(exists, reqVO, resumeKey, minioUtil, ttlSeconds);
            }
            String uploadId = UUID.randomUUID().toString();
            if (!Boolean.TRUE.equals(redisUtil.setIfAbsent(resumeKey, uploadId, ttlSeconds, TimeUnit.SECONDS))) {
                // 索引被并发请求先抢走：回到循环顶部复用赢家的会话，自己不再创建分片上传
                continue;
            }
            String objectName = buildObjectName(source, reqVO.getFileName());
            try {
                String minioUploadId = minioUtil.initMultipartUpload(objectName, reqVO.getContentType());
                InfraFileChunkSessionBO session = buildChunkSession(uploadId, minioUploadId, objectName, reqVO,
                        source, userId, chunkSize, (int) totalChunks);
                saveChunkSession(session, ttlSeconds);
                return buildChunkInitResp(session, List.of());
            } catch (RuntimeException ex) {
                // 初始化失败要把索引一起放开：否则后续重试会一直命中这条没有会话的索引
                redisUtil.delete(resumeKey);
                throw ex;
            }
        }
        throw new BizException(InfraErrorConstant.FILE_CHUNK_UPLOAD_ERROR, "初始化分片上传失败，请重试");
    }

    /**
     * 上传一个分片
     *
     * @param uploadId   会话 ID
     * @param partNumber 分片序号
     * @param file       分片内容
     * @param source     上传来源端
     * @return 分片序号与 ETag
     */
    @Override
    public FileChunkUploadRespVO uploadChunk(String uploadId, Integer partNumber, MultipartFile file,
            InfraFileSourceEnum source) {
        if (file == null || file.isEmpty()) {
            throw new BizException(InfraErrorConstant.FILE_EMPTY);
        }
        InfraFileChunkSessionBO session = requireChunkSession(uploadId, source);
        // 已合并的会话不再接受分片：再传也没有可合并的分片上传，按会话不存在处理
        if (session.getStatus() == InfraFileChunkStatusEnum.COMPLETED) {
            throw new BizException(InfraErrorConstant.FILE_CHUNK_SESSION_NOT_FOUND);
        }
        if (partNumber == null || partNumber < 1 || partNumber > session.getTotalChunks()) {
            throw new BizException(InfraErrorConstant.FILE_CHUNK_NUMBER_INVALID,
                    "分片序号必须在 1 到 " + session.getTotalChunks() + " 之间");
        }
        if (!isPartSizeValid(session, partNumber, file.getSize())) {
            throw new BizException(InfraErrorConstant.FILE_CHUNK_SIZE_INVALID,
                    "非末片不能小于 5MiB，且每片不能超过 " + toMegabytes(session.getChunkSize()) + "MB");
        }
        MinioUtil minioUtil = requireMinioUtil(InfraErrorConstant.FILE_CHUNK_UPLOAD_ERROR,
                "未配置对象存储，无法分片上传：请检查 zza.minio.endpoint");
        byte[] partBytes;
        try {
            partBytes = file.getBytes();
        } catch (IOException ex) {
            throw new BizException(InfraErrorConstant.FILE_CHUNK_UPLOAD_ERROR, "读取分片内容失败", ex);
        }
        String etag;
        try {
            etag = minioUtil.uploadPart(session.getObjectName(), session.getMinioUploadId(), partNumber, partBytes);
        } catch (RuntimeException ex) {
            log.error("分片上传失败：uploadId={}, partNumber={}", uploadId, partNumber, ex);
            throw new BizException(InfraErrorConstant.FILE_CHUNK_UPLOAD_ERROR, null, ex);
        }
        // 每传一片续期一次：大文件传到一半时不能因为会话先过期而前功尽弃
        saveChunkSession(session, infraFileProperties.getSessionExpireSeconds());
        return new FileChunkUploadRespVO(partNumber, etag);
    }

    /**
     * 合并分片并落文件记录
     *
     * @param uploadId 会话 ID
     * @param source   上传来源端
     * @return 文件记录 ID、对象名与预签名访问地址
     */
    @Override
    public FileUploadRespVO completeChunkUpload(String uploadId, InfraFileSourceEnum source) {
        InfraFileChunkSessionBO session = requireChunkSession(uploadId, source);
        MinioUtil minioUtil = requireMinioUtil(InfraErrorConstant.FILE_CHUNK_COMPLETE_ERROR,
                "未配置对象存储，无法合并分片：请检查 zza.minio.endpoint");
        if (session.getStatus() == InfraFileChunkStatusEnum.COMPLETED) {
            // 幂等：客户端重试 complete 时直接返回上次的结果，不会重复落库
            return new FileUploadRespVO(session.getFileId(), session.getObjectName(),
                    minioUtil.presignedGetUrl(session.getObjectName()));
        }
        // 分片是否齐全只认对象存储：客户端提交的 ETag 清单不可信，也不要求它提交
        Map<Integer, Long> uploadedParts = minioUtil.listUploadedParts(session.getObjectName(),
                session.getMinioUploadId());
        if (uploadedParts.size() != session.getTotalChunks()) {
            throw new BizException(InfraErrorConstant.FILE_CHUNK_INCOMPLETE,
                    "已上传 " + uploadedParts.size() + "/" + session.getTotalChunks() + " 片，请补齐后再合并");
        }
        long uploadedSize = uploadedParts.values().stream().mapToLong(Long::longValue).sum();
        if (uploadedSize != session.getSize()) {
            throw new BizException(InfraErrorConstant.FILE_CHUNK_SIZE_INVALID,
                    "分片总大小 " + uploadedSize + " 与声明的文件大小 " + session.getSize() + " 不一致");
        }
        try {
            minioUtil.completeMultipartUpload(session.getObjectName(), session.getMinioUploadId());
        } catch (RuntimeException ex) {
            log.error("合并分片失败：uploadId={}, objectName={}", uploadId, session.getObjectName(), ex);
            throw new BizException(InfraErrorConstant.FILE_CHUNK_COMPLETE_ERROR, null, ex);
        }
        InfraFile saved;
        try {
            saved = saveFileRecord(session.getName(), session.getObjectName(), session.getSize(),
                    session.getContentType(), minioUtil);
        } catch (RuntimeException ex) {
            // 合并已在对象存储生效但落库失败：saveFileRecord 已删掉对象，会话也没有重试价值，一并清掉让客户端重传
            deleteChunkSession(session, source);
            throw ex;
        }
        // 会话标记已完成并保留一段时间供幂等重试；续传索引立即释放，下次上传同一文件算新会话
        session.setStatus(InfraFileChunkStatusEnum.COMPLETED);
        session.setFileId(saved.getId());
        saveChunkSession(session, COMPLETED_SESSION_KEEP_SECONDS);
        redisUtil.delete(InfraRedisKeyUtil.fileChunkResumeKey(session.getUserId(), source, session.getFileMd5(),
                session.getSize()));
        return new FileUploadRespVO(saved.getId(), session.getObjectName(),
                minioUtil.presignedGetUrl(session.getObjectName()));
    }

    /**
     * 取消分片上传并清理已上传的分片
     *
     * @param uploadId 会话 ID
     * @param source   上传来源端
     */
    @Override
    public void abortChunkUpload(String uploadId, InfraFileSourceEnum source) {
        InfraFileChunkSessionBO session = requireChunkSession(uploadId, source);
        // 已完成的会话没有分片可取消，直接把缓存清掉即可（对象已经合并成正式文件，不能删）
        if (session.getStatus() != InfraFileChunkStatusEnum.COMPLETED) {
            MinioUtil minioUtil = requireMinioUtil(InfraErrorConstant.FILE_CHUNK_ABORT_ERROR,
                    "未配置对象存储，无法取消分片上传：请检查 zza.minio.endpoint");
            try {
                minioUtil.abortMultipartUpload(session.getObjectName(), session.getMinioUploadId());
            } catch (RuntimeException ex) {
                log.error("取消分片上传失败：uploadId={}, objectName={}", uploadId, session.getObjectName(), ex);
                throw new BizException(InfraErrorConstant.FILE_CHUNK_ABORT_ERROR, null, ex);
            }
        }
        deleteChunkSession(session, source);
    }

    /**
     * 按 ID 批量查询文件，并签发预签名访问地址
     *
     * <p>直接对每个文件签发地址，不做任何「地址缓存」：预签名地址本身带过期时间，
     * 缓存下来等于把过期时间也缓存了，调用方拿到的可能是已经失效的链接。
     *
     * @param ids 文件 ID 列表，为空时直接返回空列表
     * @return 文件列表，查不到的 ID 不返回
     */
    @Override
    public List<FileRespVO> listByIds(List<Long> ids) {
        List<Long> distinctIds = distinctIds(ids);
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        List<InfraFile> files = infraFileMapper.selectBatchIds(distinctIds);
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        MinioUtil minioUtil = requireMinioUtil(CommonErrorConstant.FILE_OPERATION_ERROR,
                "未配置对象存储，无法生成文件访问地址：请检查 zza.minio.endpoint");
        List<FileRespVO> result = new ArrayList<>(files.size());
        for (InfraFile file : files) {
            FileRespVO vo = new FileRespVO();
            vo.setId(file.getId());
            vo.setName(file.getName());
            vo.setPath(file.getPath());
            vo.setUrl(minioUtil.presignedGetUrl(file.getPath()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 用户端按 ID 批量查询文件地址
     *
     * @param ids 文件 ID 列表，可以为 null
     * @return 用户端文件列表，只含文件 ID 与访问地址
     */
    @Override
    public List<FileAppRespVO> listAppByIds(List<Long> ids) {
        return infraFileConvert.toAppVOList(listByIds(ids));
    }

    /**
     * 去重并过滤 null，避免同一次查询对同一个对象重复签发地址
     *
     * @param ids 原始 ID 列表，可以为 null
     * @return 去重后的 ID 列表
     */
    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids.stream().filter(Objects::nonNull).toList()));
    }

    /**
     * 取对象存储工具，未配置时按调用方指定的错误码报错
     *
     * <p>common-storage 只在配置了 {@code zza.minio.endpoint} 时才装配 {@link MinioUtil}，
     * 所以「没配对象存储」不是异常路径而是可预期的部署形态，必须给出明确提示而不是空指针。
     *
     * @param errorCode        未配置时使用的错误码
     * @param notConfiguredMsg 未配置时的错误提示
     * @return 对象存储工具
     */
    private MinioUtil requireMinioUtil(ErrorCode errorCode, String notConfiguredMsg) {
        MinioUtil minioUtil = minioUtilProvider.getIfAvailable();
        if (minioUtil == null) {
            log.error("{}", notConfiguredMsg);
            throw new BizException(errorCode, notConfiguredMsg);
        }
        return minioUtil;
    }

    /**
     * 保存文件记录
     *
     * <p>入库失败时把已上传的对象删掉再抛出：宁可这次上传失败让调用方重试，
     * 也不要留下一个对象存储里有、库里查不到的孤儿文件（既占空间又无法清理）。
     *
     * @param name        原始文件名
     * @param objectName  对象名
     * @param size        文件大小（字节）
     * @param contentType 内容类型
     * @param minioUtil   对象存储工具，用于补偿删除
     * @return 落库后的文件记录，含自增生成的主键
     */
    private InfraFile saveFileRecord(String name, String objectName, long size, String contentType,
            MinioUtil minioUtil) {
        InfraFile po = new InfraFile();
        po.setName(truncate(name, MAX_NAME_LENGTH));
        po.setPath(objectName);
        po.setSize(size);
        po.setContentType(truncate(contentType, MAX_CONTENT_TYPE_LENGTH));
        try {
            infraFileMapper.insert(po);
        } catch (RuntimeException ex) {
            log.error("文件记录保存失败，回滚已上传的对象：objectName={}", objectName, ex);
            try {
                minioUtil.removeObject(objectName);
            } catch (RuntimeException removeEx) {
                // 补偿删除再失败只能记日志人工处理：对象名已经打出来了，按它去对象存储里删即可
                log.error("回滚已上传的对象失败，需要人工清理：objectName={}", objectName, removeEx);
            }
            throw new BizException(InfraErrorConstant.FILE_UPLOAD_ERROR, "文件记录保存失败，请重试", ex);
        }
        return po;
    }

    /**
     * 按数据库列长度截断字符串，避免超长文件名/内容类型直接把入库打失败
     *
     * @param value     原值，可以为 null
     * @param maxLength 允许的最大长度
     * @return 截断后的字符串，入参为 null 时返回空串
     */
    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 取当前登录用户 ID，未登录直接按未授权报错
     *
     * <p>分片接口都要求登录：会话按用户绑定，未登录时拿不到用户 ID，也就无法校验会话归属。
     *
     * @return 当前登录用户 ID
     */
    private Long requireLoginUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(CommonErrorConstant.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 查询可续传的会话
     *
     * <p>续传索引还在但会话已经过期、已完成或不属于调用方时，把索引清掉再返回 null：
     * 否则后续初始化会一直命中这条用不了的索引。
     *
     * @param resumeKey 续传索引 key
     * @param userId    当前登录用户 ID
     * @param source    上传来源端
     * @return 可续传的会话，没有时返回 null
     */
    private InfraFileChunkSessionBO findResumableSession(String resumeKey, Long userId, InfraFileSourceEnum source) {
        String uploadId = redisUtil.get(resumeKey, String.class);
        if (!StringUtils.hasText(uploadId)) {
            return null;
        }
        InfraFileChunkSessionBO session = redisUtil.get(InfraRedisKeyUtil.fileChunkSessionKey(uploadId),
                InfraFileChunkSessionBO.class);
        boolean resumable = session != null
                && session.getStatus() == InfraFileChunkStatusEnum.UPLOADING
                && userId.equals(session.getUserId())
                && source.name().equals(session.getSource());
        if (resumable) {
            return session;
        }
        redisUtil.delete(resumeKey);
        return null;
    }

    /**
     * 复用已有会话继续上传：文件名与内容类型以本次请求为准，对象名与已上传的分片保持不变
     *
     * @param session    已有会话
     * @param reqVO      本次初始化入参
     * @param resumeKey  续传索引 key
     * @param minioUtil  对象存储工具，用于查询已上传的分片
     * @param ttlSeconds 会话有效期（秒）
     * @return 会话 ID、分片大小、总分片数与已上传分片序号
     */
    private FileChunkInitRespVO resumeChunkUpload(InfraFileChunkSessionBO session, FileChunkInitReqVO reqVO,
            String resumeKey, MinioUtil minioUtil, long ttlSeconds) {
        session.setName(truncate(reqVO.getFileName(), MAX_NAME_LENGTH));
        session.setContentType(truncate(reqVO.getContentType(), MAX_CONTENT_TYPE_LENGTH));
        saveChunkSession(session, ttlSeconds);
        redisUtil.set(resumeKey, session.getUploadId(), ttlSeconds, TimeUnit.SECONDS);
        // 已上传的分片以对象存储为准：缓存里只放会话元数据，不放分片进度
        List<Integer> uploadedPartNumbers = minioUtil.listUploadedPartNumbers(session.getObjectName(),
                session.getMinioUploadId());
        return buildChunkInitResp(session, uploadedPartNumbers);
    }

    /**
     * 取分片上传会话并校验归属
     *
     * <p>会话不存在、已过期，或不属于当前用户与上传端时统一按「会话不存在」处理：
     * 不区分具体原因，避免通过 uploadId 探测别人的会话。
     *
     * @param uploadId 会话 ID
     * @param source   上传来源端
     * @return 会话
     */
    private InfraFileChunkSessionBO requireChunkSession(String uploadId, InfraFileSourceEnum source) {
        Long userId = UserContextHolder.getUserId();
        InfraFileChunkSessionBO session = StringUtils.hasText(uploadId)
                ? redisUtil.get(InfraRedisKeyUtil.fileChunkSessionKey(uploadId), InfraFileChunkSessionBO.class)
                : null;
        if (session == null || userId == null || !userId.equals(session.getUserId())
                || !source.name().equals(session.getSource())) {
            throw new BizException(InfraErrorConstant.FILE_CHUNK_SESSION_NOT_FOUND);
        }
        return session;
    }

    /**
     * 组装分片上传会话
     *
     * @param uploadId      会话 ID
     * @param minioUploadId 对象存储的分片上传 ID
     * @param objectName    对象名
     * @param reqVO         初始化入参
     * @param source        上传来源端
     * @param userId        上传用户 ID
     * @param chunkSize     分片大小（字节）
     * @param totalChunks   总分片数
     * @return 分片上传会话
     */
    private InfraFileChunkSessionBO buildChunkSession(String uploadId, String minioUploadId, String objectName,
            FileChunkInitReqVO reqVO, InfraFileSourceEnum source, Long userId, long chunkSize, int totalChunks) {
        InfraFileChunkSessionBO session = new InfraFileChunkSessionBO();
        session.setUploadId(uploadId);
        session.setMinioUploadId(minioUploadId);
        session.setObjectName(objectName);
        session.setName(truncate(reqVO.getFileName(), MAX_NAME_LENGTH));
        session.setSize(reqVO.getFileSize());
        session.setContentType(truncate(reqVO.getContentType(), MAX_CONTENT_TYPE_LENGTH));
        session.setChunkSize(chunkSize);
        session.setTotalChunks(totalChunks);
        session.setSource(source.name());
        session.setUserId(userId);
        session.setFileMd5(reqVO.getFileMd5());
        session.setStatus(InfraFileChunkStatusEnum.UPLOADING);
        return session;
    }

    /**
     * 写入分片上传会话与续传索引，两者用同一个有效期
     *
     * @param session    会话
     * @param ttlSeconds 有效期（秒）
     */
    private void saveChunkSession(InfraFileChunkSessionBO session, long ttlSeconds) {
        redisUtil.set(InfraRedisKeyUtil.fileChunkSessionKey(session.getUploadId()), session, ttlSeconds,
                TimeUnit.SECONDS);
        redisUtil.set(resumeKeyOf(session), session.getUploadId(), ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 删除分片上传会话与续传索引
     *
     * @param session 会话
     * @param source  上传来源端
     */
    private void deleteChunkSession(InfraFileChunkSessionBO session, InfraFileSourceEnum source) {
        redisUtil.delete(InfraRedisKeyUtil.fileChunkSessionKey(session.getUploadId()));
        redisUtil.delete(InfraRedisKeyUtil.fileChunkResumeKey(session.getUserId(), source, session.getFileMd5(),
                session.getSize()));
    }

    /**
     * 由会话反推续传索引 key
     *
     * @param session 会话
     * @return 续传索引 key
     */
    private String resumeKeyOf(InfraFileChunkSessionBO session) {
        return InfraRedisKeyUtil.fileChunkResumeKey(session.getUserId(),
                InfraFileSourceEnum.valueOf(session.getSource()), session.getFileMd5(), session.getSize());
    }

    /**
     * 组装初始化返回体
     *
     * @param session             会话
     * @param uploadedPartNumbers 已上传的分片序号
     * @return 初始化返回体
     */
    private FileChunkInitRespVO buildChunkInitResp(InfraFileChunkSessionBO session,
            List<Integer> uploadedPartNumbers) {
        return new FileChunkInitRespVO(session.getUploadId(), session.getChunkSize(), session.getTotalChunks(),
                uploadedPartNumbers);
    }

    /**
     * 校验分片大小：分片不能为空、不能超过约定分片大小，且除末片外不能小于 5MiB
     *
     * @param session    会话
     * @param partNumber 分片序号
     * @param partSize   分片大小（字节）
     * @return 是否合法
     */
    private boolean isPartSizeValid(InfraFileChunkSessionBO session, int partNumber, long partSize) {
        if (partSize <= 0 || partSize > session.getChunkSize()) {
            return false;
        }
        return partNumber == session.getTotalChunks() || partSize >= MIN_PART_SIZE;
    }

    /**
     * 取配置的分片大小，低于对象存储要求的 5MiB 直接报错
     *
     * <p>配置错了要在第一次调用时就暴露，而不是等用户把分片都传完、合并时报 {@code EntityTooSmall}。
     *
     * @return 分片大小（字节）
     */
    private long resolveChunkSize() {
        long chunkSize = infraFileProperties.getChunkSize().toBytes();
        if (chunkSize < MIN_PART_SIZE) {
            throw new IllegalStateException(
                    "分片大小不能小于 5MiB：zza.infra.file.chunk-size=" + chunkSize);
        }
        return chunkSize;
    }

    /**
     * 字节数转 MB，仅用于拼提示信息
     *
     * @param bytes 字节数
     * @return MB 数
     */
    private long toMegabytes(long bytes) {
        return bytes / 1024 / 1024;
    }

    /**
     * 生成对象名
     *
     * @param source           上传来源端，决定目录前缀
     * @param originalFilename 原始文件名，可以为空
     * @return 对象名
     */
    private String buildObjectName(InfraFileSourceEnum source, String originalFilename) {
        return source.getDirPrefix() + "/" + LocalDate.now().format(DATE_FORMATTER) + "/"
                + UUID.randomUUID() + resolveExtension(originalFilename);
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
