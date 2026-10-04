package com.wxy.rental.biz.service.impl;

import com.wxy.common.redis.util.RedisUtil;
import com.wxy.infra.api.client.InfraAreaClient;
import com.wxy.infra.api.dto.AreaDTO;
import com.wxy.rental.biz.bo.RentalAreaCacheBO;
import com.wxy.rental.biz.constant.RentalConstant;
import com.wxy.rental.biz.service.RentalAreaService;
import com.wxy.rental.biz.util.RentalRemoteUtil;
import com.wxy.rental.biz.util.RentalRedisKeyUtil;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 行政区划服务实现：整棵区划树缓存进 Redis，本地按需要做查找。
 *
 * <p>infra 只提供「按上级查子级」与「查整棵树」两种接口，而本服务的使用场景是
 * 「按 ID 查名称」与「按市查区县」，两种都需要比父 ID 更灵活的检索，所以拉一次整棵树、
 * 扁平化后缓存，比反复按层下钻更省事也更省调用。
 *
 * <p>缓存里存扁平列表（每个节点的 children 都清空），避免同一份数据在缓存里以树形重复放大。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Slf4j
@Service
public class RentalAreaServiceImpl implements RentalAreaService {

    /** infra 行政区划读取接口 */
    @Resource
    private InfraAreaClient infraAreaClient;

    /** Redis 读写工具 */
    @Resource
    private RedisUtil redisUtil;

    /**
     * 批量取区县名称
     *
     * @param districtIds 区县 ID 集合，可以为 null
     * @return 区县 ID 到名称的映射；查不到的 ID 值为 null（打 warn 但不报错，避免一处脏数据毁掉整页）
     */
    @Override
    public Map<Long, String> getDistrictNameMap(Collection<Long> districtIds) {
        if (districtIds == null || districtIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> nameMap = allAreaNameMap();
        Map<Long, String> result = new LinkedHashMap<>();
        for (Long districtId : new LinkedHashSet<>(districtIds)) {
            if (districtId == null) {
                continue;
            }
            String name = nameMap.get(districtId);
            if (name == null) {
                log.warn("[getDistrictNameMap][区划表中找不到该 ID] districtId={}", districtId);
            }
            result.put(districtId, name);
        }
        return result;
    }

    /**
     * 把市展开成其下辖的区县 ID 列表
     *
     * @param cityId 市 ID（层级为 2 的区划），可以为 null
     * @return 区县 ID 列表，按区划树顺序；cityId 不存在或不是市时返回空列表
     */
    @Override
    public List<Long> listDistrictIdsByCity(Long cityId) {
        if (cityId == null) {
            return List.of();
        }
        List<AreaDTO> areas = allAreas();
        AreaDTO city = areas.stream().filter(area -> cityId.equals(area.getId())).findFirst().orElse(null);
        if (city == null || !RentalConstant.AREA_LEVEL_CITY.equals(city.getLevel())) {
            // 传进来的不是市级区划（例如省或区县 ID）：按查不到处理，避免返回一批语义不明的区划
            log.warn("[listDistrictIdsByCity][该 ID 不是市级区划，按查不到处理] cityId={}", cityId);
            return List.of();
        }
        List<Long> districtIds = new ArrayList<>();
        for (AreaDTO area : areas) {
            if (cityId.equals(area.getParentId()) && RentalConstant.AREA_LEVEL_DISTRICT.equals(area.getLevel())) {
                districtIds.add(area.getId());
            }
        }
        if (districtIds.isEmpty()) {
            log.warn("[listDistrictIdsByCity][该市下没有区县，按查不到处理] cityId={}", cityId);
        }
        return districtIds;
    }

    /**
     * 取「区划 ID → 名称」映射
     *
     * @return 映射
     */
    private Map<Long, String> allAreaNameMap() {
        Map<Long, String> nameMap = new LinkedHashMap<>();
        for (AreaDTO area : allAreas()) {
            if (area.getId() != null) {
                nameMap.put(area.getId(), area.getName());
            }
        }
        return nameMap;
    }

    /**
     * 取扁平化的区划列表，Redis 缓存优先
     *
     * @return 扁平化的区划列表（省、市、区县全量）
     */
    private List<AreaDTO> allAreas() {
        String cacheKey = RentalRedisKeyUtil.areaTreeKey();
        RentalAreaCacheBO cache = readCache(cacheKey);
        if (cache == null || cache.getAreas() == null) {
            List<AreaDTO> tree = RentalRemoteUtil.call(() -> infraAreaClient.listTree().requireData(), "查询行政区划");
            List<AreaDTO> areas = new ArrayList<>();
            flatten(tree, areas);
            cache = new RentalAreaCacheBO(areas);
            writeCache(cacheKey, cache);
        }
        return cache.getAreas();
    }

    /**
     * 把区划树拍平成列表
     *
     * <p>每个节点都复制成没有 children 的新对象：缓存里只留一份扁平数据，否则同一批节点会
     * 既作为某人的子节点、又作为独立节点各存一份，缓存体积会明显放大；复制而不是直接清空
     * 入参的 children，是为了不改到 infra 响应对象本身。
     *
     * @param nodes 当前层节点，可以为 null
     * @param flat  收集结果的列表
     */
    private void flatten(List<AreaDTO> nodes, List<AreaDTO> flat) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (AreaDTO node : nodes) {
            if (node == null) {
                continue;
            }
            AreaDTO flatNode = new AreaDTO();
            flatNode.setId(node.getId());
            flatNode.setParentId(node.getParentId());
            flatNode.setName(node.getName());
            flatNode.setCode(node.getCode());
            flatNode.setLevel(node.getLevel());
            flat.add(flatNode);
            flatten(node.getChildren(), flat);
        }
    }

    /**
     * 读缓存，Redis 异常时降级为回源
     *
     * @param cacheKey 缓存 key
     * @return 缓存内容，未命中或 Redis 不可用时返回 null
     */
    private RentalAreaCacheBO readCache(String cacheKey) {
        try {
            return redisUtil.get(cacheKey, RentalAreaCacheBO.class);
        } catch (RuntimeException ex) {
            log.warn("[readCache][读取区划缓存失败，降级为回源] key={}, error={}", cacheKey, ex.getMessage());
            return null;
        }
    }

    /**
     * 写缓存，失败只记日志：本次调用已经拿到数据，不该因为写缓存失败而失败
     *
     * @param cacheKey 缓存 key
     * @param cache    缓存内容
     */
    private void writeCache(String cacheKey, RentalAreaCacheBO cache) {
        try {
            redisUtil.set(cacheKey, cache, RentalConstant.AREA_TREE_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            log.warn("[writeCache][写入区划缓存失败，本次调用不受影响] key={}, error={}", cacheKey, ex.getMessage());
        }
    }
}
