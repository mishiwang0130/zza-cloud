package com.wxy.infra.biz.vo.app;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 用户端行政区划返回体：省市区三级，既作子级列表元素，也作树节点。
 *
 * <p>只暴露筛选联动要用的字段（id、名称、上级、代码、层级、子级），
 * 不带上管理端可能用到的审计字段；与 admin 的 {@code AreaRespVO} 结构相同但独立存在，
 * 两端后续各自演进时不会互相牵连。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class AreaAppRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 区划 ID */
    private Long id;

    /** 区划名称 */
    private String name;

    /** 上级区划 ID，0 表示省级 */
    private Long parentId;

    /** 行政区划代码 */
    private String code;

    /** 层级：1 省、2 市、3 区县 */
    private Integer level;

    /** 子级区划；按子级列表查询时为空列表，组建树时回填 */
    private List<AreaAppRespVO> children = new ArrayList<>();
}
