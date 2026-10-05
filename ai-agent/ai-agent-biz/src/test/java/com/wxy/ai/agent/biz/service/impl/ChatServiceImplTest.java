package com.wxy.ai.agent.biz.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wxy.ai.agent.biz.config.AiAgentProperties;
import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.ai.agent.biz.po.AiAgentConversation;
import com.wxy.ai.agent.biz.po.AiAgentMessage;
import com.wxy.ai.agent.biz.rag.RetrievalResult;
import com.wxy.ai.agent.biz.service.ConversationService;
import com.wxy.ai.agent.biz.service.RagService;
import com.wxy.ai.agent.biz.util.AiAgentRedisKeyUtil;
import com.wxy.ai.agent.biz.vo.app.ChatEvent;
import com.wxy.ai.agent.biz.vo.app.ChatStreamPayload;
import com.wxy.ai.agent.biz.vo.app.ChatStreamReqVO;
import com.wxy.common.core.context.LoginUser;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.lock.util.DistributedLockUtil;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

/**
 * 对话服务单元测试：mock 掉模型客户端与会话互斥，只验证编排逻辑。
 *
 * <p>重点守住两件事：
 * <ol>
 *   <li>流式回答结束后必须把会话互斥<b>释放掉</b>，而且用的是「可过期许可」这种能跨线程释放的原语
 *       （曾经用 Redisson 的 RLock，在 Reactor 线程释放会被静默漏放，答完一条后短时间内再问会被拒）；</li>
 *   <li>没抢到互斥时直接返回 {@code error} 事件（提示「正在回答上一条」），且不写任何消息。</li>
 * </ol>
 *
 * @author wxy
 * @date 2026/10/05
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    /** 模型客户端替身 */
    @Mock
    private ChatClient chatClient;

    /** 会话记忆替身 */
    @Mock
    private ChatMemory chatMemory;

    /** 检索服务替身 */
    @Mock
    private RagService ragService;

    /** 会话服务替身 */
    @Mock
    private ConversationService conversationService;

    /** 分布式锁替身 */
    @Mock
    private DistributedLockUtil distributedLockUtil;

    /** 被测服务 */
    private ChatServiceImpl chatService;

    /**
     * 注入替身并模拟登录用户
     */
    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl();
        ReflectionTestUtils.setField(chatService, "chatClient", chatClient);
        ReflectionTestUtils.setField(chatService, "chatMemory", chatMemory);
        ReflectionTestUtils.setField(chatService, "ragService", ragService);
        ReflectionTestUtils.setField(chatService, "conversationService", conversationService);
        ReflectionTestUtils.setField(chatService, "distributedLockUtil", distributedLockUtil);
        ReflectionTestUtils.setField(chatService, "properties", new AiAgentProperties());
        UserContextHolder.set(new LoginUser(9L, 2, "app-user"));
        // 检索与提示词拼装的细节不是本测试的关注点，统一给最小可用返回值
        lenient().when(ragService.retrieve(anyString(), any())).thenReturn(RetrievalResult.empty(false));
        lenient().when(ragService.buildUserPrompt(anyString(), any(), any())).thenReturn("prompt");
        lenient().when(ragService.toSourceList(any())).thenReturn(List.of());
    }

    /**
     * 清理登录上下文
     */
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * 流式回答结束后用可过期许可释放会话互斥
     */
    @Test
    @DisplayName("流式回答：事件顺序正确，结束后释放会话互斥（且不依赖线程绑定的 unlock）")
    void streamShouldReleasePermitAfterComplete() {
        prepareConversation();
        when(distributedLockUtil.tryAcquirePermit(anyString(), anyLong(), anyLong())).thenReturn("permit-1");
        stubStreamingAnswer("你", "好");

        List<ChatEvent> events = chatService.stream(buildRequest()).collectList().block();

        assertThat(events).isNotNull();
        assertThat(events).extracting(ChatEvent::name)
                .containsExactly(ChatEvent.EVENT_META, ChatEvent.EVENT_DELTA, ChatEvent.EVENT_DELTA,
                        ChatEvent.EVENT_SOURCES, ChatEvent.EVENT_DONE);
        verify(distributedLockUtil).releasePermit(
                eq(AiAgentRedisKeyUtil.chatLockKey(100L)), eq("permit-1"));
        // RLock 的 unlock 绑定持有线程，在 Reactor 线程释放会漏放，这里必须是 never
        verify(distributedLockUtil, never()).unlock(anyString());
    }

    /**
     * 没抢到会话互斥时返回 error 事件，并且不写消息
     */
    @Test
    @DisplayName("并发提问：没抢到会话互斥时返回 busy 的 error 事件，不落库")
    void streamShouldReturnBusyErrorWhenPermitNotAcquired() {
        AiAgentConversation conversation = new AiAgentConversation();
        conversation.setId(100L);
        conversation.setUserId(9L);
        when(conversationService.requireOwnedConversation(100L, 9L)).thenReturn(conversation);
        when(distributedLockUtil.tryAcquirePermit(anyString(), anyLong(), anyLong())).thenReturn(null);

        List<ChatEvent> events = chatService.stream(buildRequest()).collectList().block();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).name()).isEqualTo(ChatEvent.EVENT_ERROR);
        assertThat(events.get(0).data()).isInstanceOf(ChatStreamPayload.Error.class);
        assertThat(((ChatStreamPayload.Error) events.get(0).data()).code())
                .isEqualTo(AiAgentErrorConstant.CONVERSATION_BUSY.code());
        verify(conversationService, never()).insertMessage(any(AiAgentMessage.class));
    }

    /**
     * 非流式提问同样要释放会话互斥
     */
    @Test
    @DisplayName("非流式回答：结束后释放会话互斥")
    void askShouldReleasePermit() {
        prepareConversation();
        when(distributedLockUtil.tryAcquirePermit(anyString(), anyLong(), anyLong())).thenReturn("permit-1");
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("有房，两室一厅 2000 元/月");

        assertThat(chatService.ask(buildRequest()).getAnswer()).contains("2000");
        verify(distributedLockUtil).releasePermit(
                eq(AiAgentRedisKeyUtil.chatLockKey(100L)), eq("permit-1"));
    }

    /**
     * 会话归属校验与消息落库的公共桩
     */
    private void prepareConversation() {
        AiAgentConversation conversation = new AiAgentConversation();
        conversation.setId(100L);
        conversation.setUserId(9L);
        when(conversationService.requireOwnedConversation(100L, 9L)).thenReturn(conversation);
        // 记忆为空 → 触发一次历史回填（返回空历史）
        when(chatMemory.get("100")).thenReturn(List.of());
        when(conversationService.listRecentMessages(eq(100L), anyInt())).thenReturn(List.of());
        when(conversationService.insertMessage(any(AiAgentMessage.class))).thenAnswer(invocation -> {
            AiAgentMessage message = invocation.getArgument(0, AiAgentMessage.class);
            message.setId(1L);
            return message;
        });
    }

    /**
     * 流式模型调用的公共桩
     *
     * @param deltas 模型产出的增量片段
     */
    private void stubStreamingAnswer(String... deltas) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just(deltas));
    }

    /**
     * 构造提问入参：续聊已有会话
     *
     * @return 提问入参
     */
    private ChatStreamReqVO buildRequest() {
        ChatStreamReqVO reqVO = new ChatStreamReqVO();
        reqVO.setConversationId(100L);
        reqVO.setMessage("还有两室一厅的房子吗");
        return reqVO;
    }
}
