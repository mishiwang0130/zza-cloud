package com.wxy.ai.agent.biz.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理端知识库文档返回体。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "知识库文档")
public class KnowledgeDocumentRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 文档 ID */
    @Schema(description = "文档 ID")
    private Long id;

    /** 原始文件名 */
    @Schema(description = "原始文件名")
    private String fileName;

    /** 文件类型 */
    @Schema(description = "文件类型")
    private String contentType;

    /** 城市标签 */
    @Schema(description = "城市标签")
    private String city;

    /** 文件字节数 */
    @Schema(description = "文件字节数")
    private Long fileSize;

    /** 切片数量 */
    @Schema(description = "切片数量")
    private Integer chunkCount;

    /** 索引状态：0 待索引、1 已索引、2 索引失败 */
    @Schema(description = "索引状态：0 待索引、1 已索引、2 索引失败")
    private Integer status;

    /** 索引状态中文名，由 Service 回填 */
    @Schema(description = "索引状态中文名")
    private String statusName;

    /** 索引失败原因 */
    @Schema(description = "索引失败原因")
    private String errorMessage;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
