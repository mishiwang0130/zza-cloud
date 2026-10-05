package com.wxy.ai.agent.biz.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 非流式问答返回体：用于接口文档调试与脚本化回归。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "智能客服回答")
public class ChatRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID：新会话时由服务端生成 */
    @Schema(description = "会话 ID")
    private String conversationId;

    /** 完整回答 */
    @Schema(description = "完整回答")
    private String answer;

    /** 本轮命中的知识来源 */
    @Schema(description = "本轮命中的知识来源")
    private List<ChatSourceRespVO> sources;
}
