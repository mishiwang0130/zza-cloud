package com.wxy.infra.biz.vo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 新增字典类型的请求。
 *
 * @author wxy
 * @date 2026/10/04
 */
@Data
public class DictTypeCreateReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典类型名称 */
    @NotBlank(message = "字典类型名称不能为空")
    @Size(max = 100, message = "字典类型名称长度不能超过 100 个字符")
    private String name;

    /** 字典类型编码：前端按它取字典数据，服务内唯一 */
    @NotBlank(message = "字典类型编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$",
            message = "字典类型编码只能由字母、数字、下划线、中划线组成，且以字母开头")
    private String type;

    /** 状态：0 启用、1 停用，不传按启用处理 */
    private Integer status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;
}
