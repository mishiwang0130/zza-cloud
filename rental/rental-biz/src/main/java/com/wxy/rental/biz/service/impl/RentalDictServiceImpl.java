package com.wxy.rental.biz.service.impl;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.infra.api.client.InfraDictDataClient;
import com.wxy.infra.api.dto.DictDataSimpleDTO;
import com.wxy.rental.biz.bo.RentalDictCacheBO;
import com.wxy.rental.biz.constant.RentalConstant;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.service.RentalDictService;
import com.wxy.rental.biz.util.RentalCodeUtil;
import com.wxy.rental.biz.util.RentalRedisKeyUtil;
import com.wxy.rental.biz.util.RentalRemoteUtil;
import com.wxy.rental.biz.vo.admin.DictItemVO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 字典服务实现：Redis 缓存优先，未命中回源 infra。
 *
 * <p>缓存里存「一个字典类型的全部启用项」，按类型一个 key：字典是读多写少的小数据，
 * 按类型整存整取最省事，回填时在本地做映射，不必逐个编码去查。
 *
 * <p>Redis 不可用时降级为直接回源、并且不写缓存：缓存只是省一次远程调用，
 * 它挂了应该表现为「慢一点」，而不是让所有带标签的接口都失败。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Service
public class RentalDictServiceImpl implements RentalDictService {

    /** infra 字典读取接口 */
    @Resource
    private InfraDictDataClient infraDictDataClient;

    /** Redis 读写工具 */
    @Resource
    private RedisUtil redisUtil;

    /**
     * 把逗号分隔的编码串转成带中文名的字典项列表
     *
     * @param dictType 字典类型编码
     * @param codesCsv 逗号分隔的编码串，可以为 null 或空
     * @return 字典项列表，无有效编码时返回空列表
     */
    @Override
    public List<DictItemVO> listDictItems(String dictType, String codesCsv) {
        List<String> codes = RentalCodeUtil.split(codesCsv);
        if (codes.isEmpty()) {
            return List.of();
        }
        Map<String, String> labelMap = labelMap(dictType);
        List<DictItemVO> items = new ArrayList<>(codes.size());
        for (String code : codes) {
            items.add(new DictItemVO(resolveLabel(dictType, code, labelMap), code));
        }
        return items;
    }

    /**
     * 取单个编码的中文名
     *
     * @param dictType 字典类型编码
     * @param code     字典编码，可以为 null
     * @return 中文名；编码为空时返回 null，字典里查不到时按编码兜底展示
     */
    @Override
    public String getLabel(String dictType, String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return resolveLabel(dictType, code, labelMap(dictType));
    }

    /**
     * 校验编码是否都在字典里，并拼成逗号分隔的串
     *
     * @param dictType 字典类型编码
     * @param codes    编码列表，可以为 null（按空处理）
     * @return 逗号分隔的编码串，无有效编码时返回空串
     */
    @Override
    public String joinCodes(String dictType, List<String> codes) {
        String normalized = RentalCodeUtil.join(codes);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        Map<String, String> labelMap = labelMap(dictType);
        List<String> unknownCodes = RentalCodeUtil.split(normalized).stream()
                .filter(code -> !labelMap.containsKey(code))
                .toList();
        if (!unknownCodes.isEmpty()) {
            throw new BizException(RentalErrorConstant.DICT_CODE_INVALID,
                    "字典 " + dictType + " 中不存在编码：" + String.join(",", unknownCodes));
        }
        return normalized;
    }

    /**
     * 取编码对应的中文名，查不到时按编码兜底
     *
     * @param dictType 字典类型编码
     * @param code     字典编码
     * @param labelMap 编码到中文名的映射
     * @return 中文名或编码本身
     */
    private String resolveLabel(String dictType, String code, Map<String, String> labelMap) {
        String label = labelMap.get(code);
        if (label == null) {
            log.warn("[resolveLabel][字典中找不到编码，按编码展示] dictType={}, code={}", dictType, code);
            return code;
        }
        return label;
    }

    /**
     * 取字典的「编码 → 中文名」映射，Redis 缓存优先
     *
     * @param dictType 字典类型编码
     * @return 映射，按 infra 返回顺序
     */
    private Map<String, String> labelMap(String dictType) {
        String cacheKey = RentalRedisKeyUtil.dictKey(dictType);
        RentalDictCacheBO cache = readCache(cacheKey);
        if (cache == null) {
            List<DictDataSimpleDTO> items = RentalRemoteUtil.call(
                    () -> infraDictDataClient.listByType(dictType).requireData(), "查询字典");
            cache = new RentalDictCacheBO(items == null ? List.of() : items);
            writeCache(cacheKey, cache);
        }
        Map<String, String> labelMap = new LinkedHashMap<>();
        for (DictDataSimpleDTO item : cache.getItems()) {
            if (item != null && StringUtils.hasText(item.getValue())) {
                labelMap.put(item.getValue(), item.getLabel());
            }
        }
        return labelMap;
    }

    /**
     * 读缓存，Redis 异常时降级为回源
     *
     * @param cacheKey 缓存 key
     * @return 缓存内容，未命中或 Redis 不可用时返回 null
     */
    private RentalDictCacheBO readCache(String cacheKey) {
        try {
            return redisUtil.get(cacheKey, RentalDictCacheBO.class);
        } catch (RuntimeException ex) {
            log.warn("[readCache][读取字典缓存失败，降级为回源] key={}, error={}", cacheKey, ex.getMessage());
            return null;
        }
    }

    /**
     * 写缓存，失败只记日志：本次调用已经拿到数据，不该因为写缓存失败而失败
     *
     * @param cacheKey 缓存 key
     * @param cache    缓存内容
     */
    private void writeCache(String cacheKey, RentalDictCacheBO cache) {
        try {
            redisUtil.set(cacheKey, cache, RentalConstant.DICT_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            log.warn("[writeCache][写入字典缓存失败，本次调用不受影响] key={}, error={}", cacheKey, ex.getMessage());
        }
    }
}
