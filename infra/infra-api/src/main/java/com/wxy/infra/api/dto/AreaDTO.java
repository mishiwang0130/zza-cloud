package com.wxy.infra.api.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 行政区划 DTO：既是子级列表元素，也是树节点（{@code children} 有值时表示树）。
 *
 * <p>层级 {@code level} 一并返回，调用方可以按层判断该节点还能不能继续下钻，
 * 不必自己按编码位数猜。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AreaDTO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 区划 ID */
    private Long id;

    /** 上级区划 ID，0 表示省级 */
    private Long parentId;

    /** 区划名称 */
    private String name;

    /** 行政区划代码（统计局口径：省级 2 位、市级 4 位、区县 6 位） */
    private String code;

    /** 层级：1 省、2 市、3 区县 */
    private Integer level;

    /** 子级区划；按子级查询时为空列表，查树时回填 */
    private List<AreaDTO> children;
}
