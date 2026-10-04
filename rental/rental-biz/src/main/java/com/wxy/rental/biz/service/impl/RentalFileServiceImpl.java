package com.wxy.rental.biz.service.impl;

import com.wxy.common.core.exception.BizException;
import com.wxy.infra.api.client.InfraFileClient;
import com.wxy.infra.api.dto.FileRespDTO;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.service.RentalFileService;
import com.wxy.rental.biz.util.RentalRemoteUtil;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 文件服务实现：把批量查询的结果整成映射交给调用方。
 *
 * <p>刻意不缓存文件地址：地址是预签名的，缓存等于把过期时间也一起缓存，
 * 调用方可能拿到已经失效的链接；一次批量查询只调一次 infra，代价可以接受。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Service
public class RentalFileServiceImpl implements RentalFileService {

    /** infra 文件读取接口 */
    @Resource
    private InfraFileClient infraFileClient;

    /**
     * 批量换取文件访问地址
     *
     * @param fileIds 文件 ID 集合，可以为 null
     * @return 文件 ID 到预签名地址的映射；查不到的文件不放进映射
     */
    @Override
    public Map<Long, String> getFileUrlMap(Collection<Long> fileIds) {
        List<Long> distinctIds = distinctFileIds(fileIds);
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> urlMap = new LinkedHashMap<>();
        for (FileRespDTO file : listByIds(distinctIds)) {
            if (file != null && file.getId() != null) {
                urlMap.put(file.getId(), file.getUrl());
            }
        }
        return urlMap;
    }

    /**
     * 校验文件是否都真实存在
     *
     * @param fileIds 文件 ID 集合，可以为 null（按空处理）
     */
    @Override
    public void validateFilesExist(Collection<Long> fileIds) {
        List<Long> distinctIds = distinctFileIds(fileIds);
        if (distinctIds.isEmpty()) {
            return;
        }
        Set<Long> existingIds = listByIds(distinctIds).stream()
                .filter(Objects::nonNull)
                .map(FileRespDTO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> missingIds = distinctIds.stream()
                .filter(fileId -> !existingIds.contains(fileId))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new BizException(RentalErrorConstant.IMAGE_FILE_NOT_FOUND,
                    "图片文件不存在：" + missingIds);
        }
    }

    /**
     * 调 infra 按 ID 批量查文件
     *
     * @param fileIds 已去重的文件 ID 列表
     * @return 文件列表，infra 返回空时为空列表
     */
    private List<FileRespDTO> listByIds(List<Long> fileIds) {
        List<FileRespDTO> files = RentalRemoteUtil.call(
                () -> infraFileClient.listByIds(fileIds).requireData(), "查询文件");
        return files == null ? List.of() : files;
    }

    /**
     * 去重并过滤 null，避免同一次查询把重复 ID 交给 infra
     *
     * @param fileIds 原始文件 ID 集合，可以为 null
     * @return 去重后的文件 ID 列表
     */
    private List<Long> distinctFileIds(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(fileIds.stream().filter(Objects::nonNull).toList()));
    }
}
