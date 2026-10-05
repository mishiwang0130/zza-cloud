package com.wxy.infra.biz.service;

import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.vo.FileChunkInitReqVO;
import com.wxy.infra.biz.vo.FileChunkInitRespVO;
import com.wxy.infra.biz.vo.FileChunkUploadRespVO;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import com.wxy.infra.biz.vo.FileRespVO;
import com.wxy.infra.biz.vo.app.FileAppRespVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务：提供通用上传能力（上传成功后落一条 {@code infra_file} 记录），
 * 以及按 ID 批量查询（供其他服务把 {@code fileId} 换成预签名访问地址）。
 *
 * <p>刻意不做文件管理列表与删除：文件归属由业务服务自己维护，
 * 文件表只是元数据台账，开放管理端列表会让「谁的文件」变得说不清楚。
 *
 * <p>admin 端与 app 端共用同一个实现，端类型由 {@link InfraFileSourceEnum} 传入：
 * 只影响对象名的目录前缀，入库记录与返回体完全一致。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface InfraFileService {

    /**
     * 上传文件到对象存储
     *
     * @param file   上传的文件
     * @param source 上传来源端，决定对象名的目录前缀
     * @return 文件记录 ID、对象名与预签名访问地址
     */
    FileUploadRespVO upload(MultipartFile file, InfraFileSourceEnum source);

    /**
     * 初始化分片上传会话
     *
     * <p>大文件走分片上传：客户端先在这里拿到 {@code uploadId}、分片大小与总分片数，
     * 再逐片调用上传接口，最后调合并接口。同一个用户、同一端、同一 MD5 与大小的文件
     * 会复用同一个会话，续传时返回已上传的分片序号。
     *
     * @param reqVO  初始化入参（文件名、大小、内容类型、MD5）
     * @param source 上传来源端，决定对象名的目录前缀
     * @return 会话 ID、分片大小、总分片数与已上传分片序号
     */
    FileChunkInitRespVO initChunkUpload(FileChunkInitReqVO reqVO, InfraFileSourceEnum source);

    /**
     * 上传一个分片
     *
     * <p>同一个分片序号重复上传会覆盖之前的分片，客户端失败重试是安全的。
     *
     * @param uploadId   会话 ID
     * @param partNumber 分片序号，从 1 开始
     * @param file       分片内容
     * @param source     上传来源端，用于校验会话归属
     * @return 分片序号与 ETag
     */
    FileChunkUploadRespVO uploadChunk(String uploadId, Integer partNumber, MultipartFile file,
            InfraFileSourceEnum source);

    /**
     * 合并分片并落文件记录
     *
     * <p>幂等：同一个会话重复调用只会落一条记录，重试拿到的是同一份结果。
     * 分片是否齐全以对象存储为准，客户端不需要提交 ETag 清单。
     *
     * @param uploadId 会话 ID
     * @param source   上传来源端，用于校验会话归属
     * @return 文件记录 ID、对象名与预签名访问地址
     */
    FileUploadRespVO completeChunkUpload(String uploadId, InfraFileSourceEnum source);

    /**
     * 取消分片上传并清理已上传的分片
     *
     * @param uploadId 会话 ID
     * @param source   上传来源端，用于校验会话归属
     */
    void abortChunkUpload(String uploadId, InfraFileSourceEnum source);

    /**
     * 按 ID 批量查询文件，并签发预签名访问地址
     *
     * <p>查不到的 ID 直接不返回，由调用方按「文件不存在」处理；重复 ID 会先去重，
     * 避免同一次查询对同一对象重复签发地址。
     *
     * @param ids 文件 ID 列表，为空时直接返回空列表
     * @return 文件列表，按传入顺序不保证
     */
    List<FileRespVO> listByIds(List<Long> ids);

    /**
     * 用户端按 ID 批量查询文件地址（只返回文件 ID 与预签名访问地址）
     *
     * <p>匿名可访问：访客端拿到的房源图片只有 {@code fileId}，需要靠它换成可展示地址；
     * 查不到的 ID 不返回，入参为空时返回空列表。
     *
     * @param ids 文件 ID 列表，为空时直接返回空列表
     * @return 用户端文件列表
     */
    List<FileAppRespVO> listAppByIds(List<Long> ids);
}
