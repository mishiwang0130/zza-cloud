package com.wxy.rental.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.result.Result;
import com.wxy.common.redis.util.RedisUtil;
import com.wxy.infra.api.client.InfraDictDataClient;
import com.wxy.infra.api.dto.DictDataSimpleDTO;
import com.wxy.rental.biz.bo.RentalDictCacheBO;
import com.wxy.rental.biz.constant.RentalConstant;
import com.wxy.rental.biz.constant.RentalErrorConstant;
import com.wxy.rental.biz.vo.DictItemVO;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 字典服务单元测试：缓存优先、回源、编码兜底与写库前校验。
 *
 * @author wxy
 * @date 2026/10/04
 */
@ExtendWith(MockitoExtension.class)
class RentalDictServiceImplTest {

    /** 字典类型编码 */
    private static final String DICT_TYPE = "rental_apartment_label";

    /** 缓存 key */
    private static final String CACHE_KEY = "zza:rental:dict:" + DICT_TYPE;

    /** infra 字典接口 */
    @Mock
    private InfraDictDataClient infraDictDataClient;

    /** Redis 工具 */
    @Mock
    private RedisUtil redisUtil;

    /** 被测服务 */
    private RentalDictServiceImpl dictService;

    /**
     * 装配被测服务
     */
    @BeforeEach
    void setUp() {
        dictService = new RentalDictServiceImpl();
        ReflectionTestUtils.setField(dictService, "infraDictDataClient", infraDictDataClient);
        ReflectionTestUtils.setField(dictService, "redisUtil", redisUtil);
    }

    /**
     * 缓存未命中时回源 infra，并把结果写进缓存
     */
    @Test
    @DisplayName("listDictItems：缓存未命中时回源并写缓存，未知编码按编码兜底")
    void listDictItemsShouldLoadRemoteAndCache() {
        when(redisUtil.get(CACHE_KEY, RentalDictCacheBO.class)).thenReturn(null);
        when(infraDictDataClient.listByType(DICT_TYPE)).thenReturn(
                Result.success(List.of(new DictDataSimpleDTO("近地铁", "near_subway"))));

        List<DictItemVO> items = dictService.listDictItems(DICT_TYPE, "near_subway,unknown_code");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getLabel()).isEqualTo("近地铁");
        assertThat(items.get(0).getValue()).isEqualTo("near_subway");
        assertThat(items.get(1).getLabel()).isEqualTo("unknown_code");

        ArgumentCaptor<RentalDictCacheBO> captor = ArgumentCaptor.forClass(RentalDictCacheBO.class);
        verify(redisUtil).set(eq(CACHE_KEY), captor.capture(),
                eq(RentalConstant.DICT_CACHE_SECONDS), eq(TimeUnit.SECONDS));
        assertThat(captor.getValue().getItems()).hasSize(1);
    }

    /**
     * 缓存命中时不再调 infra
     */
    @Test
    @DisplayName("listDictItems：缓存命中时不回源")
    void listDictItemsShouldUseCache() {
        when(redisUtil.get(CACHE_KEY, RentalDictCacheBO.class)).thenReturn(
                new RentalDictCacheBO(List.of(new DictDataSimpleDTO("品牌公寓", "brand_apartment"))));

        List<DictItemVO> items = dictService.listDictItems(DICT_TYPE, "brand_apartment");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getLabel()).isEqualTo("品牌公寓");
        verifyNoInteractions(infraDictDataClient);
    }

    /**
     * 写库前校验：编码都在字典里时去重拼接
     */
    @Test
    @DisplayName("joinCodes：编码合法时去重拼接")
    void joinCodesShouldNormalizeValidCodes() {
        when(redisUtil.get(CACHE_KEY, RentalDictCacheBO.class)).thenReturn(
                new RentalDictCacheBO(List.of(
                        new DictDataSimpleDTO("近地铁", "near_subway"),
                        new DictDataSimpleDTO("品牌公寓", "brand_apartment"))));

        String codes = dictService.joinCodes(DICT_TYPE, List.of("brand_apartment", "near_subway", "brand_apartment"));

        assertThat(codes).isEqualTo("brand_apartment,near_subway");
    }

    /**
     * 写库前校验：存在字典里没有的编码时报「字典编码不存在或已停用」
     */
    @Test
    @DisplayName("joinCodes：存在未知编码时报字典编码不合法")
    void joinCodesShouldRejectUnknownCode() {
        when(redisUtil.get(CACHE_KEY, RentalDictCacheBO.class)).thenReturn(
                new RentalDictCacheBO(List.of(new DictDataSimpleDTO("近地铁", "near_subway"))));

        assertThatThrownBy(() -> dictService.joinCodes(DICT_TYPE, List.of("near_subway", "gone")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(RentalErrorConstant.DICT_CODE_INVALID.code()));
    }

    /**
     * 批量回填：一次加载字典、按行转换，未知编码按编码兜底
     */
    @Test
    @DisplayName("listDictItemsBatch：一次加载字典并批量回填")
    void listDictItemsBatchShouldConvertEachCsv() {
        when(redisUtil.get(CACHE_KEY, RentalDictCacheBO.class)).thenReturn(
                new RentalDictCacheBO(List.of(new DictDataSimpleDTO("近地铁", "near_subway"))));

        Map<String, List<DictItemVO>> result = dictService.listDictItemsBatch(DICT_TYPE,
                List.of("near_subway,gone", "near_subway", ""));

        assertThat(result.get("near_subway,gone")).extracting(DictItemVO::getLabel)
                .containsExactly("近地铁", "gone");
        assertThat(result.get("near_subway")).hasSize(1);
        assertThat(result.get("")).isEmpty();
        // 整个批量只读一次缓存，不回源
        verify(infraDictDataClient, times(0)).listByType(DICT_TYPE);
    }

    /**
     * 批量回填：入参为空时直接返回空映射
     */
    @Test
    @DisplayName("listDictItemsBatch：入参为空时返回空映射")
    void listDictItemsBatchShouldReturnEmptyForBlankInput() {
        assertThat(dictService.listDictItemsBatch(DICT_TYPE, null)).isEmpty();
        assertThat(dictService.listDictItemsBatch(DICT_TYPE, List.of())).isEmpty();

        verifyNoInteractions(redisUtil);
    }
}
