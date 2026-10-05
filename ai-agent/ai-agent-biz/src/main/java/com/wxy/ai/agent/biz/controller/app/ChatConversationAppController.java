package com.wxy.ai.agent.biz.controller.app;

import com.wxy.ai.agent.biz.service.ConversationService;
import com.wxy.ai.agent.biz.vo.app.ConversationDeleteReqVO;
import com.wxy.ai.agent.biz.vo.app.ConversationRespVO;
import com.wxy.ai.agent.biz.vo.app.MessageRespVO;
import com.wxy.common.core.result.Result;
import com.wxy.common.core.vo.PageReqVO;
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
 * 小程序端会话接口：最终对外路径为 {@code /api/ai-agent/app-api/chat/conversation/...}。
 *
 * <p>只操作自己的会话：会话归属由 Service 按登录用户校验，别人的会话一律按「会话不存在」返回。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Tag(name = "用户端 - 智能客服会话")
@RestController
@RequestMapping("/chat/conversation")
public class ChatConversationAppController {

    /** 会话服务 */
    @Resource
    private ConversationService conversationService;

    /**
     * 分页查询我的会话
     *
     * @param reqVO 分页入参
     * @return 会话分页
     */
    @Operation(summary = "分页查询我的会话", description = "按最后消息时间倒序，只查自己")
    @PostMapping("/page")
    public Result<PageRespVO<ConversationRespVO>> page(@Validated @RequestBody PageReqVO reqVO) {
        return Result.success(conversationService.pageMyConversation(reqVO));
    }

    /**
     * 拉取会话消息
     *
     * @param conversationId 会话 ID
     * @param lastId         只取 id 大于它的消息，可为空
     * @param size           拉取条数，可为空
     * @return 消息列表
     */
    @Operation(summary = "拉取会话消息", description = "按 id 升序，支持 lastId 增量拉取；用于进入会话时回放历史")
    @GetMapping("/messages")
    public Result<List<MessageRespVO>> messages(
            @Parameter(description = "会话 ID") @RequestParam Long conversationId,
            @Parameter(description = "只取 id 大于它的消息") @RequestParam(required = false) Long lastId,
            @Parameter(description = "拉取条数") @RequestParam(required = false) Integer size) {
        return Result.success(conversationService.listMyMessages(conversationId, lastId, size));
    }

    /**
     * 删除会话
     *
     * @param reqVO 删除入参
     * @return 空响应
     */
    @Operation(summary = "删除会话", description = "逻辑删除会话并清掉 Redis 会话记忆；消息保留，便于后台追溯")
    @PostMapping("/delete")
    public Result<Void> delete(@Validated @RequestBody ConversationDeleteReqVO reqVO) {
        conversationService.deleteMyConversation(reqVO.getConversationId());
        return Result.success();
    }
}
