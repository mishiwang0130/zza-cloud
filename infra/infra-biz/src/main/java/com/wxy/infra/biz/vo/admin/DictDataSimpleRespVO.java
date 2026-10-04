package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典数据精简返回体：前端下拉/标签展示用，只给「标签 + 值」。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDataSimpleRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典标签，展示用 */
    private String label;

    /** 字典值，存库与传参用 */
    private String value;
}
