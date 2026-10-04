package com.wxy.rental.biz.bo;

import com.wxy.infra.api.dto.DictDataSimpleDTO;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典缓存值：某个字典类型下启用的「标签 + 值」列表。
 *
 * <p>包一层对象的原因与 {@code RentalAreaCacheBO} 相同：{@code RedisUtil} 按类型反序列化，
 * 泛型 List 擦除后会还原成裸 Map，包一层才能拿到强类型列表。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalDictCacheBO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典数据列表（只含启用项，按 infra 的排序号升序） */
    private List<DictDataSimpleDTO> items;
}
