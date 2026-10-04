package com.wxy.rental.biz.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 行政区划服务：把公寓 / 房间上的区县 ID 变成能展示的名称，并把「市」展开成区县 ID 列表。
 *
 * <p>区划数据只有 infra 有，本服务不改它，只读；读取结果本地缓存（区划是标准数据几乎不变），
 * 否则公寓列表每行都要为了一个区县名去调一次 infra。
 *
 * @author wxy
 * @date 2026/10/04
 */
public interface RentalAreaService {

    /**
     * 批量取区县名称（公寓列表与详情回填用）
     *
     * @param districtIds 区县 ID 集合，可以为 null
     * @return 区县 ID 到名称的映射；查不到的 ID 值为 null
     */
    Map<Long, String> getDistrictNameMap(Collection<Long> districtIds);

    /**
     * 把市展开成其下辖的区县 ID 列表（公寓列表按 cityId 筛选用）
     *
     * @param cityId 市 ID（层级为 2 的区划），可以为 null
     * @return 区县 ID 列表，按区划树顺序；cityId 不存在或不是市时返回空列表
     */
    List<Long> listDistrictIdsByCity(Long cityId);
}
