package com.wxy.ai.agent.biz.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 语义检索调试返回的单条命中：带完整片段正文，便于人工判断检索质量。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "语义检索命中片段")
public class KnowledgeSearchItemRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识库文档 ID（字符串形式） */
    @Schema(description = "知识库文档 ID")
    private String documentId;

    /** 来源文件名 */
    @Schema(description = "来源文件名")
    private String fileName;

    /** 城市标签 */
    @Schema(description = "城市标签")
    private String city;

    /** 相似度得分 */
    @Schema(description = "相似度得分")
    private Double score;

    /** 片段正文 */
    @Schema(description = "片段正文")
    private String text;
}
