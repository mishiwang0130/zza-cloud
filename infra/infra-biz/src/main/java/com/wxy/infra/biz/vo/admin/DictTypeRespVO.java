package com.wxy.infra.biz.vo.admin;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 字典类型返回体。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class DictTypeRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典类型 ID */
    private Long id;

    /** 字典类型名称 */
    private String name;

    /** 字典类型编码 */
    private String type;

    /** 状态：0 启用、1 停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
