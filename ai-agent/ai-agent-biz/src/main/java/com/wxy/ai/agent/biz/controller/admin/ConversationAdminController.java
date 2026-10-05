package com.wxy.ai.agent.biz.controller.admin;

import com.wxy.ai.agent.biz.constant.AiAgentPermissionConstant;
import com.wxy.ai.agent.biz.service.ConversationService;
import com.wxy.ai.agent.biz.vo.admin.ConversationAdminRespVO;
import com.wxy.ai.agent.biz.vo.admin.ConversationMessageRespVO;
import com.wxy.ai.agent.biz.vo.admin.ConversationPageReqVO;
import com.wxy.common.core.result.Result;
import com.wxy.common.core.security.RequiresPermission;
import com.wxy.common.core.vo.PageRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台会话记录接口：最终对外路径为 {@code /api/ai-agent/admin-api/conversation/...}。
 *
 * <p>只读：智能客服不做人工接管，后台的作用是排查「用户投诉的那次回答是怎么来的」，
 * 所以列表带上用户 ID 与消息条数，明细带上命中的知识来源与耗时。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Tag(name = "管理后台 - 智能客服会话")
@RestController
@RequestMapping("/conversation")
public class ConversationAdminController {

    /** 会话服务 */
    @Resource
    private ConversationService conversationService;

    /**
     * 分页查询会话
     *
     * @param reqVO 分页入参
     * @return 会话分页
     */
    @Operation(summary = "分页查询会话记录", description = "可按用户 ID 与标题关键字过滤")
    @RequiresPermission(AiAgentPermissionConstant.CONVERSATION_QUERY)
    @PostMapping("/page")
    public Result<PageRespVO<ConversationAdminRespVO>> page(
            @Validated @RequestBody ConversationPageReqVO reqVO) {
        return Result.success(conversationService.pageConversation(reqVO));
    }

    /**
     * 查询会话消息明细
     *
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    @Operation(summary = "查询会话消息明细", description = "按时间正序返回，含命中的知识来源、模型与耗时")
    @RequiresPermission(AiAgentPermissionConstant.CONVERSATION_QUERY)
    @GetMapping("/messages")
    public Result<List<ConversationMessageRespVO>> messages(
            @Parameter(description = "会话 ID") @RequestParam Long conversationId) {
        return Result.success(conversationService.listConversationMessages(conversationId));
    }
}
