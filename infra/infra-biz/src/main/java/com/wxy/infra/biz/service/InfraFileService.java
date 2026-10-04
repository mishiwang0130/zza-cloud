package com.wxy.infra.biz.service;

import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务：目前只提供通用上传能力（上传成功后落一条 {@code infra_file} 记录），
 * 不做文件列表与删除等管理功能。
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
}
