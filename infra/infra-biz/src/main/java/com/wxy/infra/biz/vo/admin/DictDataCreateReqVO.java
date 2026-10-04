package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 新增字典数据的请求。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class DictDataCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属字典类型编码 */
    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型编码长度不能超过 100 个字符")
    private String dictType;

    /** 字典标签，展示用 */
    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签长度不能超过 100 个字符")
    private String label;

    /** 字典值，存库与传参用；同一类型下唯一 */
    @NotBlank(message = "字典值不能为空")
    @Size(max = 100, message = "字典值长度不能超过 100 个字符")
    private String value;

    /** 排序号，越小越靠前，不传按 0 处理 */
    private Integer sort;

    /** 状态：0 启用、1 停用，不传按启用处理 */
    private Integer status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;
}
