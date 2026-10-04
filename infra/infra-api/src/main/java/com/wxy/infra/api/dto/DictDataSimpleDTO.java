package com.wxy.infra.api.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典数据精简 DTO：跨服务取字典时只给「字典值 + 中文标签」。
 *
 * <p>只返回这两项是刻意的：调用方（例如 rental 回填标签中文名）拿到后直接组装自己的返回体，
 * 不需要状态、排序号、备注这些维护端字段，少传一点就少一份对 infra 内部结构的耦合。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDataSimpleDTO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典标签，展示用 */
    private String label;

    /** 字典值，存库与传参用 */
    private String value;
}
