package com.wxy.infra.biz.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wxy.common.mybatis.po.BasePO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型表 {@code infra_dict_type} 的实体。
 *
 * <p>编码 {@code type} 是前端取字典数据的依据，服务内唯一；修改编码时要同步更新
 * {@code infra_dict_data.dict_type}，否则已有字典数据会变成孤儿。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_dict_type")
public class InfraDictType extends BasePO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典类型名称，展示用 */
    private String name;

    /** 字典类型编码，服务内唯一（按未删除数据判断），前端按它取字典数据 */
    private String type;

    /** 状态：0 启用、1 停用，取值见 {@code CommonStatusEnum}；停用后不再对外提供字典数据 */
    private Integer status;

    /** 备注 */
    private String remark;
}
