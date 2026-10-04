package com.wxy.rental.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典项返回体：库里的编码 + 字典里的中文名。
 *
 * <p>中文名由 Service 查 infra 字典后回填，前端拿到的就是可以直接展示的数据，
 * 不需要再为了显示一个标签去查一次字典。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典中文名 */
    private String label;

    /** 字典编码（入库与传参用的值） */
    private String value;
}
