package com.wxy.rental.biz.service;

import java.util.Collection;
import java.util.Map;

/**
 * 文件服务：把 rental 库里存的 {@code fileId} 换成可直接展示的预签名地址。
 *
 * <p>rental 只存文件 ID：预签名地址会过期，落库就是脏数据；展示前换一次即可，
 * 地址过期由下一次查询自然刷新。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalFileService {

    /**
     * 批量换取文件访问地址
     *
     * @param fileIds 文件 ID 集合，可以为 null
     * @return 文件 ID 到预签名地址的映射；查不到的文件不放进映射
     */
    Map<Long, String> getFileUrlMap(Collection<Long> fileIds);

    /**
     * 校验文件是否都真实存在（图片随表单提交时用）
     *
     * <p>不校验的后果是图片关系表里留下一批查不到文件的 {@code file_id}，
     * 详情页会出现空白图，而且只能靠人工比对文件表才能定位。
     *
     * @param fileIds 文件 ID 集合，可以为 null（按空处理）
     * @throws com.wxy.common.core.exception.BizException 存在查不到的文件时抛出
     */
    void validateFilesExist(Collection<Long> fileIds);
}
