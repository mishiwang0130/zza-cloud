package com.wxy.infra.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型分页查询入参。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypePageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典类型名称，前后模糊匹配 */
    private String name;

    /** 字典类型编码，前后模糊匹配 */
    private String type;

    /** 状态：0 启用、1 停用，为空表示全部 */
    private Integer status;
}
