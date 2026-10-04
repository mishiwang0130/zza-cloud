package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 修改字典数据的请求：允许把数据挪到别的字典类型下（会校验新类型下字典值不重复）。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class DictDataUpdateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典数据 ID */
    @NotNull(message = "字典数据 ID 不能为空")
    private Long id;

    /** 所属字典类型编码 */
    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型编码长度不能超过 100 个字符")
    private String dictType;

    /** 字典标签 */
    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签长度不能超过 100 个字符")
    private String label;

    /** 字典值 */
    @NotBlank(message = "字典值不能为空")
    @Size(max = 100, message = "字典值长度不能超过 100 个字符")
    private String value;

    /** 排序号 */
    private Integer sort;

    /** 状态：0 启用、1 停用 */
    private Integer status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;
}
