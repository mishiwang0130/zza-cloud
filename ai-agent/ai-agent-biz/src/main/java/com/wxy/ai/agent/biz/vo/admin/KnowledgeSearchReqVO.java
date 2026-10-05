package com.wxy.ai.agent.biz.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 管理端语义检索调试入参：用于对比「同一个问题在过滤城市前后」的召回差异。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "语义检索调试入参")
public class KnowledgeSearchReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 检索问题 */
    @Schema(description = "检索问题", example = "押金怎么算")
    @NotBlank(message = "检索问题不能为空")
    @Size(max = 500, message = "检索问题长度不能超过 500")
    private String query;

    /** 召回条数：不传取配置的默认值 */
    @Schema(description = "召回条数")
    @Min(value = 1, message = "召回条数至少为 1")
    @Max(value = 20, message = "召回条数最多为 20")
    private Integer topK;

    /** 城市标签：为空表示不按城市过滤 */
    @Schema(description = "城市标签")
    @Size(max = 32, message = "城市标签长度不能超过 32")
    private String city;
}
