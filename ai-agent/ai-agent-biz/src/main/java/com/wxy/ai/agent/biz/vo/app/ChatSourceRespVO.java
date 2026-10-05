package com.wxy.ai.agent.biz.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 本轮回答命中的知识库来源：给小程序端展示「回答依据」，也用于后台排查检索质量。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "知识库来源片段")
public class ChatSourceRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识库文档 ID（字符串形式，避免前端精度问题） */
    @Schema(description = "知识库文档 ID")
    private String documentId;

    /** 来源文件名 */
    @Schema(description = "来源文件名", example = "租赁规则.md")
    private String fileName;

    /** 相似度得分，越大越相似 */
    @Schema(description = "相似度得分", example = "0.82")
    private Double score;

    /** 片段摘要，用于前端展示与人工排查 */
    @Schema(description = "片段摘要")
    private String snippet;
}
