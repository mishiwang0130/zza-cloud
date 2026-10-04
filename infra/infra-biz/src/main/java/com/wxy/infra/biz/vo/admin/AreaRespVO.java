package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 行政区划返回体：既是子级列表元素，也是树节点（{@code children} 由 Service 用 {@code TreeUtil} 组装）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AreaRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 区划 ID */
    private Long id;

    /** 上级区划 ID，0 表示省级 */
    private Long parentId;

    /** 区划名称 */
    private String name;

    /** 行政区划代码 */
    private String code;

    /** 层级：1 省、2 市、3 区县 */
    private Integer level;

    /** 子级区划；按子级列表查询时为空列表，组建树时回填 */
    private List<AreaRespVO> children;
}
