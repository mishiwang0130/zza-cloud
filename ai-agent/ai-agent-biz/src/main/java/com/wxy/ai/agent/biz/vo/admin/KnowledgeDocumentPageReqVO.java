package com.wxy.ai.agent.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端知识库文档分页入参。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库文档分页入参")
public class KnowledgeDocumentPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件名关键字，模糊匹配；空白视为不过滤 */
    @Schema(description = "文件名关键字")
    private String fileName;

    /** 城市标签：通用 表示平台级文档 */
    @Schema(description = "城市标签")
    private String city;

    /** 索引状态：0 待索引、1 已索引、2 索引失败 */
    @Schema(description = "索引状态：0 待索引、1 已索引、2 索引失败")
    private Integer status;
}
