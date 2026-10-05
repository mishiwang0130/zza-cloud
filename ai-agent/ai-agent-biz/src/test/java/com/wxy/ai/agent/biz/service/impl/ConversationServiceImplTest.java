package com.wxy.ai.agent.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxy.ai.agent.biz.config.AiAgentProperties;
import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.ai.agent.biz.convert.AiAgentConversationConvert;
import com.wxy.ai.agent.biz.convert.AiAgentMessageConvert;
import com.wxy.ai.agent.biz.mapper.AiAgentConversationMapper;
import com.wxy.ai.agent.biz.mapper.AiAgentMessageMapper;
import com.wxy.ai.agent.biz.po.AiAgentConversation;
import com.wxy.ai.agent.biz.vo.app.ConversationRespVO;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.vo.PageReqVO;
import com.wxy.common.core.vo.PageRespVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 会话服务单元测试：mock Mapper 与转换器，不连数据库与 Redis。
 *
 * <p>重点守住「跨用户不可见」：查别人的会话必须按「会话不存在」处理，不能因为 ID 猜对就返回数据。
 *
 * @author wxy
 * @date 2026/10/05
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    /** 会话 Mapper 替身 */
    @Mock
    private AiAgentConversationMapper conversationMapper;

    /** 消息 Mapper 替身 */
    @Mock
    private AiAgentMessageMapper messageMapper;

    /** 会话转换器替身 */
    @Mock
    private AiAgentConversationConvert conversationConvert;

    /** 消息转换器替身 */
    @Mock
    private AiAgentMessageConvert messageConvert;

    /** 会话记忆提供者替身 */
    @Mock
    private ObjectProvider<ChatMemory> chatMemoryProvider;

    /** 被测服务 */
    private ConversationServiceImpl conversationService;

    /**
     * 注入替身并模拟登录用户
     */
    @BeforeEach
    void setUp() {
        conversationService = new ConversationServiceImpl();
        ReflectionTestUtils.setField(conversationService, "aiAgentConversationMapper", conversationMapper);
        ReflectionTestUtils.setField(conversationService, "aiAgentMessageMapper", messageMapper);
        ReflectionTestUtils.setField(conversationService, "aiAgentConversationConvert", conversationConvert);
        ReflectionTestUtils.setField(conversationService, "aiAgentMessageConvert", messageConvert);
        ReflectionTestUtils.setField(conversationService, "properties", new AiAgentProperties());
        ReflectionTestUtils.setField(conversationService, "chatMemoryProvider", chatMemoryProvider);
        UserContextHolder.set(new LoginUser(9L, 2, "app-user"));
    }

    /**
     * 清理登录上下文，避免线程复用串号
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 会话不存在或不属于当前用户时都报「会话不存在」
     */
    @Test
    @DisplayName("归属校验：不存在与别人的会话都报会话不存在")
    void requireOwnedConversationShouldRejectMissingOrOthers() {
        when(conversationMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> conversationService.requireOwnedConversation(1L, 9L))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getCode())
                .isEqualTo(AiAgentErrorConstant.CONVERSATION_NOT_FOUND.code());

        AiAgentConversation others = new AiAgentConversation();
        others.setId(2L);
        others.setUserId(8L);
        when(conversationMapper.selectById(2L)).thenReturn(others);
        assertThatThrownBy(() -> conversationService.requireOwnedConversation(2L, 9L))
                .isInstanceOf(BizException.class);
    }

    /**
     * 新建会话时初始化标题、消息条数与最后消息时间
     */
    @Test
    @DisplayName("新建会话：初始化标题与统计字段")
    void createConversationShouldInitFields() {
        when(conversationMapper.insert(any(AiAgentConversation.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, AiAgentConversation.class).setId(100L);
            return 1;
        });

        Long id = conversationService.createConversation(9L, "两室一厅多少钱");

        assertThat(id).isEqualTo(100L);
        ArgumentCaptor<AiAgentConversation> captor = ArgumentCaptor.forClass(AiAgentConversation.class);
        verify(conversationMapper).insert(captor.capture());
        AiAgentConversation saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(9L);
        assertThat(saved.getTitle()).isEqualTo("两室一厅多少钱");
        assertThat(saved.getMessageCount()).isZero();
        assertThat(saved.getLastMessageTime()).isNotNull();
    }

    /**
     * 我的会话分页：返回转换后的记录与总数
     */
    @Test
    @DisplayName("我的会话分页：返回转换后的记录")
    void pageMyConversationShouldReturnConvertedRecords() {
        AiAgentConversation row = new AiAgentConversation();
        row.setId(1L);
        Page<AiAgentConversation> page = new Page<>(1, 20);
        page.setRecords(List.of(row));
        page.setTotal(1L);
        when(conversationMapper.selectPage(any(Page.class), any())).thenReturn(page);
        ConversationRespVO vo = new ConversationRespVO();
        vo.setId(1L);
        when(conversationConvert.toRespVOList(anyList())).thenReturn(List.of(vo));

        PageRespVO<ConversationRespVO> result = conversationService.pageMyConversation(new PageReqVO());

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getId()).isEqualTo(1L);
    }
}
