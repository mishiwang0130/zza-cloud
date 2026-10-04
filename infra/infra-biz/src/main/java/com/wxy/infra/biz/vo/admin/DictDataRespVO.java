package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 字典数据返回体。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class DictDataRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典数据 ID */
    private Long id;

    /** 所属字典类型编码 */
    private String dictType;

    /** 字典标签 */
    private String label;

    /** 字典值 */
    private String value;

    /** 排序号 */
    private Integer sort;

    /** 状态：0 启用、1 停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
