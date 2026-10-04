package com.wxy.infra.biz.vo.app;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端字典数据返回体：只给「标签 + 值」，供前端下拉 / 标签展示。
 *
 * <p>不带状态、排序号、备注等维护端字段：访客端只关心能直接渲染的文案与回传的编码。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDataAppRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典标签，展示用 */
    private String label;

    /** 字典值，存库与传参用 */
    private String value;
}
