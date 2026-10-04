package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行政区划表 {@code infra_area} 的实体：省 / 市 / 区县三级自关联（{@code parent_id} 指向上级）。
 *
 * <p>数据是标准区划，由脚本 {@code sql/infra_area.sql} 维护，不提供增删改接口；
 * 编码 {@code code} 用统计局口径（省级 2 位、市级 4 位、区县 6 位），全国唯一。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_area")
public class InfraArea extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 上级区划 ID，0 表示省级；索引 {@code idx_infra_area_parent_id} */
    private Long parentId;

    /** 区划名称 */
    private String name;

    /** 行政区划代码，唯一索引 {@code uk_infra_area_code} */
    private String code;

    /** 层级：1 省、2 市、3 区县，取值见 {@code InfraAreaLevelEnum} */
    private Integer level;
}
