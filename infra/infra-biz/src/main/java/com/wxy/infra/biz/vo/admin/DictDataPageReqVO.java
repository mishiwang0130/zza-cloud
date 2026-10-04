package com.wxy.infra.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据分页查询入参：按类型查是这个页面的主场景，所以类型编码是精确匹配。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictDataPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属字典类型编码，精确匹配 */
    private String dictType;

    /** 字典标签，前后模糊匹配 */
    private String label;

    /** 状态：0 启用、1 停用，为空表示全部 */
    private Integer status;
}
