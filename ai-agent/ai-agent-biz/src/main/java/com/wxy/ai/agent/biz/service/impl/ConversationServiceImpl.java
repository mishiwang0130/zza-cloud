package com.wxy.ai.agent.biz.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.ai.agent.biz.config.AiAgentProperties;
import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.ai.agent.biz.convert.AiAgentConversationConvert;
import com.wxy.ai.agent.biz.convert.AiAgentMessageConvert;
import com.wxy.ai.agent.biz.mapper.AiAgentConversationMapper;
import com.wxy.ai.agent.biz.mapper.AiAgentMessageMapper;
import com.wxy.ai.agent.biz.po.AiAgentConversation;
import com.wxy.ai.agent.biz.po.AiAgentMessage;
import com.wxy.ai.agent.biz.service.ConversationService;
import com.wxy.ai.agent.biz.vo.admin.ConversationAdminRespVO;
import com.wxy.ai.agent.biz.vo.admin.ConversationMessageRespVO;
import com.wxy.ai.agent.biz.vo.admin.ConversationPageReqVO;
import com.wxy.ai.agent.biz.vo.app.ChatSourceRespVO;
import com.wxy.ai.agent.biz.vo.app.ConversationRespVO;
import com.wxy.ai.agent.biz.vo.app.MessageRespVO;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import com.wxy.common.mybatis.util.PageUtil;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 会话与消息实现：所有查询都按登录用户过滤，跨用户访问一律按「会话不存在」处理。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {

    /** 会话默认拉取条数 */
    private static final int DEFAULT_MESSAGE_SIZE = 50;

    /** 会话 Mapper */
    @Resource
    private AiAgentConversationMapper aiAgentConversationMapper;

    /** 消息 Mapper */
    @Resource
    private AiAgentMessageMapper aiAgentMessageMapper;

    /** 会话转换器 */
    @Resource
    private AiAgentConversationConvert aiAgentConversationConvert;

    /** 消息转换器 */
    @Resource
    private AiAgentMessageConvert aiAgentMessageConvert;

    /** 业务配置 */
    @Resource
    private AiAgentProperties properties;

    /** 会话记忆：删除会话时一并清掉，避免 Redis 里留下孤儿上下文 */
    @Resource
    private ObjectProvider<ChatMemory> chatMemoryProvider;

    /**
     * 新建会话
     *
     * @param userId 所属用户 ID
     * @param title  会话标题
     * @return 新会话 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(Long userId, String title) {
        LocalDateTime now = LocalDateTime.now();
        AiAgentConversation conversation = new AiAgentConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setMessageCount(0);
        // 一开始就有值：会话列表按最后消息时间倒序，留着 null 会让新建但还没回答的会话排序不确定
        conversation.setLastMessageTime(now);
        aiAgentConversationMapper.insert(conversation);
        return conversation.getId();
    }

    /**
     * 校验会话归属
     *
     * @param conversationId 会话 ID
     * @param userId         当前登录用户 ID
     * @return 会话实体
     */
    @Override
    public AiAgentConversation requireOwnedConversation(Long conversationId, Long userId) {
        AiAgentConversation conversation = conversationId == null
                ? null : aiAgentConversationMapper.selectById(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) {
            throw new BizException(AiAgentErrorConstant.CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    /**
     * 落库一条消息
     *
     * @param message 消息实体
     * @return 带主键的消息实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgentMessage insertMessage(AiAgentMessage message) {
        aiAgentMessageMapper.insert(message);
        return message;
    }

    /**
     * 本轮问答结束：消息条数 +2 并刷新最后消息时间
     *
     * @param conversationId  会话 ID
     * @param lastMessageTime 最后一条消息时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishTurn(Long conversationId, LocalDateTime lastMessageTime) {
        aiAgentConversationMapper.increaseMessageCount(conversationId, 2, lastMessageTime);
    }

    /**
     * 取会话最近若干条消息
     *
     * @param conversationId 会话 ID
     * @param limit          最多取多少条
     * @return 消息列表，按时间正序
     */
    @Override
    public List<AiAgentMessage> listRecentMessages(Long conversationId, int limit) {
        Page<AiAgentMessage> page = new Page<>(1, Math.max(limit, 1));
        IPage<AiAgentMessage> result = aiAgentMessageMapper.selectPage(page,
                new LambdaQueryWrapper<AiAgentMessage>()
                        .eq(AiAgentMessage::getConversationId, conversationId)
                        .orderByDesc(AiAgentMessage::getId));
        List<AiAgentMessage> records = new ArrayList<>(result.getRecords());
        // 查询按 id 倒序取「最近 N 条」，回填上下文要按时间正序，所以在这里翻正
        Collections.reverse(records);
        return records;
    }

    /**
     * 分页查询我的会话
     *
     * @param reqVO 分页入参
     * @return 会话分页
     */
    @Override
    public PageRespVO<ConversationRespVO> pageMyConversation(PageReqVO reqVO) {
        Long userId = requireLoginUserId();
        Page<AiAgentConversation> page = PageUtil.toPage(reqVO);
        IPage<AiAgentConversation> result = aiAgentConversationMapper.selectPage(page,
                new LambdaQueryWrapper<AiAgentConversation>()
                        .eq(AiAgentConversation::getUserId, userId)
                        .orderByDesc(AiAgentConversation::getLastMessageTime)
                        .orderByDesc(AiAgentConversation::getId));
        return PageUtil.of(result, aiAgentConversationConvert.toRespVOList(result.getRecords()));
    }

    /**
     * 拉取我的会话消息
     *
     * @param conversationId 会话 ID
     * @param lastId         只取 id 大于它的消息，可为空
     * @param size           拉取条数，可为空取默认值
     * @return 消息列表
     */
    @Override
    public List<MessageRespVO> listMyMessages(Long conversationId, Long lastId, Integer size) {
        Long userId = requireLoginUserId();
        requireOwnedConversation(conversationId, userId);
        int limit = normalizeSize(size);
        Page<AiAgentMessage> page = new Page<>(1, limit);
        IPage<AiAgentMessage> result = aiAgentMessageMapper.selectPage(page,
                new LambdaQueryWrapper<AiAgentMessage>()
                        .eq(AiAgentMessage::getConversationId, conversationId)
                        .gt(lastId != null, AiAgentMessage::getId, lastId)
                        .orderByAsc(AiAgentMessage::getId));
        List<MessageRespVO> records = aiAgentMessageConvert.toRespVOList(result.getRecords());
        for (int i = 0; i < records.size(); i++) {
            records.get(i).setSources(parseSources(result.getRecords().get(i).getSources()));
        }
        return records;
    }

    /**
     * 逻辑删除我的会话
     *
     * @param conversationId 会话 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyConversation(Long conversationId) {
        Long userId = requireLoginUserId();
        requireOwnedConversation(conversationId, userId);
        // deleteById 走 @TableLogic，只置 is_delete=1；消息保留，便于后台追溯
        aiAgentConversationMapper.deleteById(conversationId);
        ChatMemory chatMemory = chatMemoryProvider.getIfAvailable();
        if (chatMemory != null) {
            chatMemory.clear(String.valueOf(conversationId));
        }
    }

    /**
     * 管理端分页查询会话
     *
     * @param reqVO 分页入参
     * @return 会话分页
     */
    @Override
    public PageRespVO<ConversationAdminRespVO> pageConversation(ConversationPageReqVO reqVO) {
        Page<AiAgentConversation> page = PageUtil.toPage(reqVO);
        IPage<AiAgentConversation> result = aiAgentConversationMapper.selectPage(page,
                new LambdaQueryWrapper<AiAgentConversation>()
                        .eq(reqVO.getUserId() != null, AiAgentConversation::getUserId, reqVO.getUserId())
                        .like(StringUtils.hasText(reqVO.getKeyword()),
                                AiAgentConversation::getTitle, reqVO.getKeyword())
                        .orderByDesc(AiAgentConversation::getLastMessageTime)
                        .orderByDesc(AiAgentConversation::getId));
        return PageUtil.of(result, aiAgentConversationConvert.toAdminRespVOList(result.getRecords()));
    }

    /**
     * 管理端查看会话消息明细
     *
     * @param conversationId 会话 ID
     * @return 消息列表，按时间正序
     */
    @Override
    public List<ConversationMessageRespVO> listConversationMessages(Long conversationId) {
        List<AiAgentMessage> messages = aiAgentMessageMapper.selectList(
                new LambdaQueryWrapper<AiAgentMessage>()
                        .eq(AiAgentMessage::getConversationId, conversationId)
                        .orderByAsc(AiAgentMessage::getId));
        List<ConversationMessageRespVO> records = aiAgentMessageConvert.toAdminRespVOList(messages);
        for (int i = 0; i < records.size(); i++) {
            records.get(i).setSources(parseSources(messages.get(i).getSources()));
        }
        return records;
    }

    /**
     * 解析库里存的来源 JSON
     *
     * @param sourcesJson JSON 字符串，可为空
     * @return 来源列表；解析失败时返回空列表并记日志，不让历史消息接口整体失败
     */
    private List<ChatSourceRespVO> parseSources(String sourcesJson) {
        if (!StringUtils.hasText(sourcesJson)) {
            return List.of();
        }
        try {
            List<ChatSourceRespVO> sources = JSON.parseArray(sourcesJson, ChatSourceRespVO.class);
            return sources == null ? List.of() : sources;
        } catch (Exception ex) {
            log.warn("解析消息来源 JSON 失败：{}", ex.getMessage());
            return List.of();
        }
    }

    /**
     * 收敛拉取条数
     *
     * @param size 入参条数
     * @return 合法条数
     */
    private int normalizeSize(Integer size) {
        int max = properties.getChat().getMaxHistorySize();
        if (size == null || size < 1) {
            return DEFAULT_MESSAGE_SIZE;
        }
        return Math.min(size, max);
    }

    /**
     * 取当前登录用户 ID
     *
     * @return 登录用户 ID
     */
    private Long requireLoginUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }
}
