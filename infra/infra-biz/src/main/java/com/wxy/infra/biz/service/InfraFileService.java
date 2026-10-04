package com.wxy.infra.biz.service;

import com.wxy.infra.biz.enums.InfraFileSourceEnum;
import com.wxy.infra.biz.vo.FileUploadRespVO;
import com.wxy.infra.biz.vo.admin.FileRespVO;
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
     * 按 ID 批量查询文件，并签发预签名访问地址
     *
     * <p>查不到的 ID 直接不返回，由调用方按「文件不存在」处理；重复 ID 会先去重，
     * 避免同一次查询对同一对象重复签发地址。
     *
     * @param ids 文件 ID 列表，为空时直接返回空列表
     * @return 文件列表，按传入顺序不保证
     */
    List<FileRespVO> listByIds(List<Long> ids);
}
