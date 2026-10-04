package com.wxy.rental.biz.bo;

import com.wxy.infra.api.dto.AreaDTO;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行政区划缓存值：扁平化的省市区列表。
 *
 * <p>为什么不直接缓存 {@code List<AreaDTO>}：{@code RedisUtil} 按类型反序列化，
 * 泛型 List 的运行时类型擦除会让 Fastjson2 还原成裸 Map。包一层对象后类型信息明确，
 * 读取时可以直接拿到强类型列表。
 *
 * <p>缓存扁平列表而不是树，是因为两个使用场景都只需要「按 ID 找节点」与「按父 ID 找子节点」，
 * 扁平结构的查找最直接；需要树形展示的是 App 端前端，那是后续窗口的事。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalAreaCacheBO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 扁平化的区划列表（省、市、区县全量） */
    private List<AreaDTO> areas;
}
