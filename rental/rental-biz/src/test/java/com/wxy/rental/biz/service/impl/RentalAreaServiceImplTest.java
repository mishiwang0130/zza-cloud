package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.result.Result;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.infra.api.client.InfraAreaClient;
import com.wxy.infra.api.dto.AreaDTO;
import com.wxy.rental.biz.bo.RentalAreaCacheBO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 区划服务单元测试：整棵树拍平缓存、按 ID 取名、按市展开区县。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalAreaServiceImplTest {

    /** 区划缓存 key */
    private static final String CACHE_KEY = "zza:rental:area:tree";

    /** infra 区划接口 */
    @Mock
    private InfraAreaClient infraAreaClient;

    /** Redis 工具 */
    @Mock
    private RedisUtil redisUtil;

    /** 被测服务 */
    private RentalAreaServiceImpl areaService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        areaService = new RentalAreaServiceImpl();
        ReflectionTestUtils.setField(areaService, "infraAreaClient", infraAreaClient);
        ReflectionTestUtils.setField(areaService, "redisUtil", redisUtil);
    }

    /**
     * 缓存未命中时拉整棵树、拍平后按 ID / 父 ID 检索
     */
    @Test
    @DisplayName("回源：拍平区划树后按 ID 取名、按市展开区县")
    void shouldFlattenTreeAndQueryLocally() {
        when(redisUtil.get(CACHE_KEY, RentalAreaCacheBO.class)).thenReturn(null);
        when(infraAreaClient.listTree()).thenReturn(Result.success(buildTree()));

        Map<Long, String> nameMap = areaService.getDistrictNameMap(List.of(3L, 2L));
        List<Long> districtIds = areaService.listDistrictIdsByCity(2L);

        assertThat(nameMap).containsEntry(3L, "西湖区").containsEntry(2L, "杭州市");
        assertThat(districtIds).containsExactly(3L);
    }

    /**
     * 缓存命中时不再调 infra；查不到的 ID 值为 null 而不是报错
     */
    @Test
    @DisplayName("缓存命中：不回源，查不到的 ID 返回 null")
    void shouldUseCacheAndTolerateUnknownId() {
        AreaDTO cached = buildArea(3L, 2L, 3, "西湖区");
        when(redisUtil.get(CACHE_KEY, RentalAreaCacheBO.class))
                .thenReturn(new RentalAreaCacheBO(List.of(cached)));

        assertThat(areaService.getDistrictNameMap(List.of(3L, 999L)))
                .containsEntry(3L, "西湖区")
                .containsEntry(999L, null);
        assertThat(areaService.listDistrictIdsByCity(null)).isEmpty();
        verifyNoInteractions(infraAreaClient);
    }

    /**
     * 构造省 - 市 - 区县三级树
     *
     * @return 树
     */
    private List<AreaDTO> buildTree() {
        AreaDTO district = buildArea(3L, 2L, 3, "西湖区");
        AreaDTO city = buildArea(2L, 1L, 2, "杭州市");
        city.setChildren(List.of(district));
        AreaDTO province = buildArea(1L, 0L, 1, "浙江省");
        province.setChildren(List.of(city));
        return List.of(province);
    }

    /**
     * 构造区划节点
     *
     * @param id       区划 ID
     * @param parentId 上级区划 ID
     * @param level    层级
     * @param name     名称
     * @return 区划节点
     */
    private AreaDTO buildArea(Long id, Long parentId, Integer level, String name) {
        AreaDTO area = new AreaDTO();
        area.setId(id);
        area.setParentId(parentId);
        area.setLevel(level);
        area.setName(name);
        return area;
    }
}
