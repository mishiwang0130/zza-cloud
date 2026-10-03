package com.wxy.infra.biz.service;

import com.wxy.infra.biz.vo.admin.FileUploadRespVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务：目前只提供通用上传能力，不做文件列表与删除等管理功能。
 *
 * @author wxy
 * @date 2026/10/03
 */
public interface InfraFileService {

    /**
     * 上传文件到对象存储
     *
     * @param file 上传的文件
     * @return 对象名与预签名访问地址
     */
    FileUploadRespVO upload(MultipartFile file);
}
