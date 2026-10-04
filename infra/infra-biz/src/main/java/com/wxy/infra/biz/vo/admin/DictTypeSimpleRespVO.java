package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 字典类型精简返回体：新增/修改字典数据时的类型下拉用，值为编码、标签为名称。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class DictTypeSimpleRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典类型 ID */
    private Long id;

    /** 字典类型编码，作为下拉的取值 */
    private String type;

    /** 字典类型名称，作为下拉的展示文案 */
    private String name;
}
