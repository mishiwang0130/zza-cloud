package com.wxy.ai.agent.biz.vo.admin;

import com.wxy.ai.agent.biz.vo.app.ChatSourceRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 管理端会话消息明细：比小程序端多带 userId，便于按用户排查问题。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Data
@Schema(description = "管理端会话消息")
public class ConversationMessageRespVO implements Serializable {

    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息 ID */
    @Schema(description = "消息 ID")
    private Long id;

    /** 所属用户 ID */
    @Schema(description = "所属用户 ID")
    private Long userId;

    /** 发送方：1 用户、2 智能客服 */
    @Schema(description = "发送方：1 用户、2 智能客服")
    private Integer senderType;

    /** 消息内容 */
    @Schema(description = "消息内容")
    private String content;

    /** 命中的知识来源 */
    @Schema(description = "命中的知识来源")
    private List<ChatSourceRespVO> sources;

    /** 使用的模型 */
    @Schema(description = "使用的模型")
    private String model;

    /** 回答耗时（毫秒） */
    @Schema(description = "回答耗时（毫秒）")
    private Integer latencyMs;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
