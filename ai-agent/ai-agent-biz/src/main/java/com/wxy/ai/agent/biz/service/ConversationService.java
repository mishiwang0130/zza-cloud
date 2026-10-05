package com.wxy.ai.agent.biz.service;

import com.wxy.ai.agent.biz.po.AiAgentConversation;
import com.wxy.ai.agent.biz.po.AiAgentMessage;
import com.wxy.ai.agent.biz.vo.admin.ConversationAdminRespVO;
import com.wxy.ai.agent.biz.vo.admin.ConversationMessageRespVO;
import com.wxy.ai.agent.biz.vo.admin.ConversationPageReqVO;
import com.wxy.ai.agent.biz.vo.app.ConversationRespVO;
import com.wxy.ai.agent.biz.vo.app.MessageRespVO;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话与消息能力：小程序端回放自己的会话，管理端查看会话记录，以及问答链路要用的落库动作。
 *
 * <p>模型的多轮上下文不在这里：那由 Spring AI 的 Redis 会话记忆维护。本服务只管
 * 「历史与审计」这一份数据，两者互不覆盖写。
 *
 * @author wxy
 * @date 2026/10/05
 */
public interface ConversationService {

    /**
     * 新建会话：标题取首条用户问题截断，最后消息时间初始化成当前时间以保证列表排序稳定
     *
     * @param userId 所属用户 ID
     * @param title  会话标题（已截断）
     * @return 新会话 ID
     */
    Long createConversation(Long userId, String title);

    /**
     * 校验会话存在且属于当前用户
     *
     * <p>不存在与不属于自己都报「会话不存在」：不区分原因，避免用别人的会话 ID 试探存在性。
     *
     * @param conversationId 会话 ID
     * @param userId         当前登录用户 ID
     * @return 会话实体
     */
    AiAgentConversation requireOwnedConversation(Long conversationId, Long userId);

    /**
     * 落库一条消息
     *
     * @param message 消息实体
     * @return 带主键的消息实体
     */
    AiAgentMessage insertMessage(AiAgentMessage message);

    /**
     * 本轮问答结束：消息条数 +2（用户 + AI）并刷新最后消息时间
     *
     * @param conversationId  会话 ID
     * @param lastMessageTime 最后一条消息时间
     */
    void finishTurn(Long conversationId, LocalDateTime lastMessageTime);

    /**
     * 取会话最近若干条消息（按 id 倒序取，再翻正），用于 Redis 会话记忆失效后回填上下文
     *
     * @param conversationId 会话 ID
     * @param limit          最多取多少条
     * @return 消息列表，按时间正序
     */
    List<AiAgentMessage> listRecentMessages(Long conversationId, int limit);

    /**
     * 分页查询我的会话
     *
     * @param reqVO 分页入参
     * @return 会话分页
     */
    PageRespVO<ConversationRespVO> pageMyConversation(PageReqVO reqVO);

    /**
     * 拉取我的会话消息（按 id 升序，支持 lastId 增量拉取）
     *
     * @param conversationId 会话 ID
     * @param lastId         只取 id 大于它的消息，可为空
     * @param size           拉取条数，可为空取默认值
     * @return 消息列表
     */
    List<MessageRespVO> listMyMessages(Long conversationId, Long lastId, Integer size);

    /**
     * 逻辑删除我的会话
     *
     * @param conversationId 会话 ID
     */
    void deleteMyConversation(Long conversationId);

    /**
     * 管理端分页查询会话
     *
     * @param reqVO 分页入参
     * @return 会话分页
     */
    PageRespVO<ConversationAdminRespVO> pageConversation(ConversationPageReqVO reqVO);

    /**
     * 管理端查看会话消息明细
     *
     * @param conversationId 会话 ID
     * @return 消息列表，按时间正序
     */
    List<ConversationMessageRespVO> listConversationMessages(Long conversationId);
}
