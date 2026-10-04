package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据表 {@code infra_dict_data} 的实体：一个字典类型下的若干「标签 - 值」。
 *
 * <p>按类型编码（{@code dict_type}）而不是类型 ID 关联：前端与后端都是拿编码取字典，
 * 这样取数据是一次单表查询；代价是改编码时要同步刷数据（见 {@code InfraDictTypeService}）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_dict_data")
public class InfraDictData extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属字典类型编码，索引 {@code idx_infra_dict_data_dict_type} */
    private String dictType;

    /** 字典标签，展示用 */
    private String label;

    /** 字典值，存库与传参用；同一类型下唯一（按未删除数据判断） */
    private String value;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 状态：0 启用、1 停用，取值见 {@code CommonStatusEnum}；停用后不下发给前端 */
    private Integer status;

    /** 备注 */
    private String remark;
}
