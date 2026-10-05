package com.wxy.ai.agent.biz.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 按文档 ID 操作（重建索引 / 删除）的入参。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "知识库文档操作入参")
public class KnowledgeDocumentIdReqVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 文档 ID */
    @Schema(description = "文档 ID")
    @NotNull(message = "文档 ID 不能为空")
    private Long id;
}
