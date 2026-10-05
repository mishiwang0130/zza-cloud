package com.wxy.ai.agent.biz.vo.admin;

import com.wxy.common.core.vo.PageReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端会话分页入参。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "会话分页入参")
public class ConversationPageReqVO extends PageReqVO {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID：按某个小程序用户过滤 */
    @Schema(description = "用户 ID")
    private Long userId;

    /** 标题关键字，模糊匹配；空白视为不过滤 */
    @Schema(description = "标题关键字")
    private String keyword;
}
