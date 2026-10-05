package com.wxy.ai.agent.biz.service.impl;

import com.alibaba.fastjson2.JSON;
import com.wxy.ai.agent.biz.config.AiAgentProperties;
import com.wxy.ai.agent.biz.constant.AiAgentConstant;
import com.wxy.ai.agent.biz.constant.AiAgentErrorConstant;
import com.wxy.ai.agent.biz.enums.AiAgentMessageSenderEnum;
import com.wxy.ai.agent.biz.po.AiAgentConversation;
import com.wxy.ai.agent.biz.po.AiAgentMessage;
import com.wxy.ai.agent.biz.rag.RetrievalResult;
import com.wxy.ai.agent.biz.service.ChatService;
import com.wxy.ai.agent.biz.service.ConversationService;
import com.wxy.ai.agent.biz.service.RagService;
import com.wxy.ai.agent.biz.util.AiAgentRedisKeyUtil;
import com.wxy.ai.agent.biz.vo.app.ChatEvent;
import com.wxy.ai.agent.biz.vo.app.ChatRespVO;
import com.wxy.ai.agent.biz.vo.app.ChatSourceRespVO;
import com.wxy.ai.agent.biz.vo.app.ChatStreamPayload;
import com.wxy.ai.agent.biz.vo.app.ChatStreamReqVO;
import com.wxy.common.core.context.UserContextHolder;
import com.wxy.common.core.exception.BizException;
import com.wxy.common.core.exception.UnauthorizedException;
import com.wxy.common.core.result.CommonErrorConstant;
import com.wxy.common.lock.util.DistributedLockUtil;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * 对话主流程：加锁 → 落库用户消息 → 检索知识库 → 模型流式输出 → 落库 AI 消息。
 *
 * <p><b>两个存储的分工</b>：多轮上下文由 Spring AI 的 Redis 会话记忆按 conversationId 维护；
 * MySQL 存的是历史与审计（小程序端回放、后台排查）。两者互不覆盖写。
 *
 * <p><b>为什么先加分布式锁</b>：同一会话并发提问会让两条回答交错写进同一份记忆，
 * 上下文直接乱掉；多实例部署下本地锁拦不住，所以用 Redisson 的分布式锁。
 * 拿不到锁立即返回「正在回答上一条消息」，不做排队。
 *
 * <p><b>锁为什么用「可过期许可」而不是 RLock</b>：加锁发生在 Servlet 请求线程，而流结束时
 * 的释放发生在 Reactor 线程（{@code doFinally}）；Redisson 的 {@code RLock} 绑定持有线程，
 * 换线程释放会被当成「不是自己持的锁」而静默漏放，导致回答完之后一段时间内同一个会话一直被拒。
 * 所以这里用 {@code DistributedLockUtil.tryAcquirePermit/releasePermit}：许可不属于线程，
 * 且带租期，既能跨线程释放，也能在进程崩溃时自动过期。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    /** 当前使用的模型名：只用于落库记录，便于排查「换个模型后效果变差」 */
    @Value("${spring.ai.openai.chat.options.model:unknown}")
    private String chatModelName;

    /** 会话记忆窗口：与 Spring AI 的配置保持一致，用于记忆失效后的历史回填 */
    @Value("${spring.ai.memory.max-messages:20}")
    private int memoryWindowSize;

    /** ChatClient：系统提示词、记忆顾问与工具已在配置类里挂好 */
    @Resource
    private ChatClient chatClient;

    /** 会话记忆（Redis 实现） */
    @Resource
    private ChatMemory chatMemory;

    /** 知识库检索 */
    @Resource
    private RagService ragService;

    /** 会话与消息落库 */
    @Resource
    private ConversationService conversationService;

    /** 分布式锁 */
    @Resource
    private DistributedLockUtil distributedLockUtil;

    /** 业务配置 */
    @Resource
    private AiAgentProperties properties;

    /**
     * 流式提问
     *
     * @param reqVO 提问入参
     * @return 事件流
     */
    @Override
    public Flux<ChatEvent> stream(ChatStreamReqVO reqVO) {
        ChatContext context;
        try {
            context = prepare(reqVO, requireLoginUserId());
        } catch (RuntimeException ex) {
            // 流还没开始时就把失败转成 error 事件：小程序端只需按事件名分发，不用兼容 JSON 错误体
            log.warn("智能客服流式提问前置校验失败：{}", ex.getMessage());
            return Flux.just(toErrorEvent(ex));
        }
        return Flux.defer(() -> doStream(context))
                // 检索与模型调用都是阻塞式网络 IO，切到弹性线程池执行，避免占住 Servlet 请求线程
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    logModelFailure(context.conversationId(), ex);
                    return Flux.just(toErrorEvent(ex));
                })
                // 无论正常结束、异常还是客户端断开，都要把锁放掉（租期只是兜底）
                .doFinally(signalType -> distributedLockUtil.releasePermit(context.lockKey(), context.permitId()));
    }

    /**
     * 非流式提问
     *
     * @param reqVO 提问入参
     * @return 完整回答
     */
    @Override
    public ChatRespVO ask(ChatStreamReqVO reqVO) {
        ChatContext context = prepare(reqVO, requireLoginUserId());
        try {
            long startedAt = System.nanoTime();
            RetrievalResult retrieval = ragService.retrieve(context.message(), context.city());
            List<ChatSourceRespVO> sources = ragService.toSourceList(retrieval.chunks());
            String answer = requestAnswer(context, retrieval);
            saveAiMessage(context, sources, answer, startedAt);

            ChatRespVO resp = new ChatRespVO();
            resp.setConversationId(context.conversationIdKey());
            resp.setAnswer(answer);
            resp.setSources(sources);
            return resp;
        } finally {
            distributedLockUtil.releasePermit(context.lockKey(), context.permitId());
        }
    }

    /**
     * 流式回答：检索 → 模型增量 → 落库 → 结束事件
     *
     * @param context 本轮上下文
     * @return 事件流
     */
    private Flux<ChatEvent> doStream(ChatContext context) {
        long startedAt = System.nanoTime();
        RetrievalResult retrieval = ragService.retrieve(context.message(), context.city());
        List<ChatSourceRespVO> sources = ragService.toSourceList(retrieval.chunks());
        StringBuilder answer = new StringBuilder();

        Flux<ChatEvent> deltaFlux = chatClient.prompt()
                .user(ragService.buildUserPrompt(context.message(), context.city(), retrieval))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, context.conversationIdKey()))
                .toolContext(Map.of(AiAgentConstant.TOOL_CONTEXT_USER_ID, context.userId()))
                .stream()
                .content()
                // 边推给前端边攒全文：流结束后要整段落库，不需要再调一次模型
                .doOnNext(answer::append)
                .map(delta -> ChatEvent.of(ChatEvent.EVENT_DELTA, new ChatStreamPayload.Delta(delta)));

        return Flux.concat(
                Flux.just(ChatEvent.of(ChatEvent.EVENT_META,
                        new ChatStreamPayload.Meta(context.conversationIdKey(), context.userMessageId()))),
                deltaFlux,
                Flux.defer(() -> {
                    String text = answer.toString();
                    if (!StringUtils.hasText(text)) {
                        throw new BizException(AiAgentErrorConstant.MODEL_ERROR);
                    }
                    AiAgentMessage aiMessage = saveAiMessage(context, sources, text, startedAt);
                    return Flux.just(
                            ChatEvent.of(ChatEvent.EVENT_SOURCES, new ChatStreamPayload.Sources(sources)),
                            ChatEvent.of(ChatEvent.EVENT_DONE, new ChatStreamPayload.Done(
                                    context.conversationIdKey(), aiMessage.getId(), elapsedMillis(startedAt))));
                }));
    }

    /**
     * 非流式调用模型
     *
     * @param context   本轮上下文
     * @param retrieval 检索结果
     * @return 完整回答文本
     */
    private String requestAnswer(ChatContext context, RetrievalResult retrieval) {
        String answer = chatClient.prompt()
                .user(ragService.buildUserPrompt(context.message(), context.city(), retrieval))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, context.conversationIdKey()))
                .toolContext(Map.of(AiAgentConstant.TOOL_CONTEXT_USER_ID, context.userId()))
                .call()
                .content();
        if (!StringUtils.hasText(answer)) {
            throw new BizException(AiAgentErrorConstant.MODEL_ERROR);
        }
        return answer;
    }

    /**
     * 前置准备：校验、定会话、加锁、回填记忆、落库用户消息
     *
     * @param reqVO  提问入参
     * @param userId 当前登录用户 ID
     * @return 本轮上下文
     */
    private ChatContext prepare(ChatStreamReqVO reqVO, Long userId) {
        if (reqVO == null) {
            throw new BizException(CommonErrorConstant.PARAM_ERROR, "请求不能为空");
        }
        String message = normalizeMessage(reqVO.getMessage());
        String city = StringUtils.hasText(reqVO.getCity()) ? reqVO.getCity().trim() : null;
        AiAgentConversation conversation = resolveConversation(reqVO.getConversationId(), userId, message);

        String lockKey = AiAgentRedisKeyUtil.chatLockKey(conversation.getId());
        AiAgentProperties.Lock lock = properties.getLock();
        // 用带租期的许可（而不是 RLock）：本轮回答在 Reactor 线程结束，释放必须能跨线程
        String permitId = distributedLockUtil.tryAcquirePermit(lockKey,
                lock.getWaitMillis(), lock.getChatLeaseMillis());
        if (permitId == null) {
            throw new BizException(AiAgentErrorConstant.CONVERSATION_BUSY);
        }
        try {
            // 先回填记忆再落库本轮用户消息：回填取的是「本轮之前」的历史，
            // 本轮问题由记忆顾问自己追加，避免同一句话在提示词里出现两次
            warmUpMemory(conversation.getId());
            AiAgentMessage userMessage = insertUserMessage(conversation.getId(), userId, message);
            return new ChatContext(conversation.getId(), userId, message, city, lockKey, permitId,
                    userMessage.getId());
        } catch (RuntimeException ex) {
            distributedLockUtil.releasePermit(lockKey, permitId);
            throw ex;
        }
    }

    /**
     * 定会话：没传 ID 就新建，传了就校验归属
     *
     * @param conversationId 会话 ID，可为空
     * @param userId         当前登录用户 ID
     * @param message        本轮问题（用于新会话标题）
     * @return 会话实体
     */
    private AiAgentConversation resolveConversation(Long conversationId, Long userId, String message) {
        if (conversationId != null) {
            return conversationService.requireOwnedConversation(conversationId, userId);
        }
        Long newId = conversationService.createConversation(userId, buildTitle(message));
        return conversationService.requireOwnedConversation(newId, userId);
    }

    /**
     * 落库用户消息
     *
     * @param conversationId 会话 ID
     * @param userId         用户 ID
     * @param message        问题文本
     * @return 带主键的消息
     */
    private AiAgentMessage insertUserMessage(Long conversationId, Long userId, String message) {
        AiAgentMessage userMessage = new AiAgentMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setUserId(userId);
        userMessage.setSenderType(AiAgentMessageSenderEnum.USER.getValue());
        userMessage.setContent(message);
        userMessage.setSources("");
        return conversationService.insertMessage(userMessage);
    }

    /**
     * 落库 AI 消息并结算本轮
     *
     * @param context   本轮上下文
     * @param sources   命中的知识来源
     * @param answer    回答全文
     * @param startedAt 开始时间（纳秒）
     * @return 带主键的消息
     */
    private AiAgentMessage saveAiMessage(ChatContext context, List<ChatSourceRespVO> sources,
                                         String answer, long startedAt) {
        AiAgentMessage aiMessage = new AiAgentMessage();
        aiMessage.setConversationId(context.conversationId());
        aiMessage.setUserId(context.userId());
        aiMessage.setSenderType(AiAgentMessageSenderEnum.AI.getValue());
        aiMessage.setContent(answer);
        aiMessage.setSources(sources == null || sources.isEmpty() ? "" : JSON.toJSONString(sources));
        aiMessage.setModel(chatModelName);
        aiMessage.setLatencyMs((int) Math.round(elapsedMillis(startedAt)));
        conversationService.insertMessage(aiMessage);
        conversationService.finishTurn(context.conversationId(), LocalDateTime.now());
        return aiMessage;
    }

    /**
     * Redis 会话记忆为空时，用 MySQL 历史回填最近若干条
     *
     * <p>作用：会话超过记忆 TTL 后用户再来提问，模型仍然记得之前的对话，
     * 而不是「历史记录还在、模型却失忆」。
     *
     * @param conversationId 会话 ID
     */
    private void warmUpMemory(Long conversationId) {
        String conversationIdKey = String.valueOf(conversationId);
        List<Message> memory = chatMemory.get(conversationIdKey);
        if (memory != null && !memory.isEmpty()) {
            return;
        }
        List<AiAgentMessage> history = conversationService.listRecentMessages(conversationId, memoryWindowSize);
        List<Message> messages = new ArrayList<>(history.size());
        for (AiAgentMessage item : history) {
            if (!StringUtils.hasText(item.getContent())) {
                continue;
            }
            if (AiAgentMessageSenderEnum.USER.getValue().equals(item.getSenderType())) {
                messages.add(new UserMessage(item.getContent()));
            } else if (AiAgentMessageSenderEnum.AI.getValue().equals(item.getSenderType())) {
                messages.add(new AssistantMessage(item.getContent()));
            }
        }
        if (!messages.isEmpty()) {
            chatMemory.add(conversationIdKey, messages);
            log.debug("会话 {} 的 Redis 记忆为空，已用 {} 条历史回填", conversationId, messages.size());
        }
    }

    /**
     * 校验并规整问题文本
     *
     * @param message 原始问题
     * @return 规整后的问题
     */
    private String normalizeMessage(String message) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            throw new BizException(CommonErrorConstant.PARAM_ERROR, "问题不能为空");
        }
        int maxLength = properties.getChat().getMaxMessageLength();
        if (trimmed.length() > maxLength) {
            throw new BizException(CommonErrorConstant.PARAM_ERROR, "问题长度不能超过 " + maxLength + " 字");
        }
        return trimmed;
    }

    /**
     * 生成会话标题：取首条问题截断，不调用模型
     *
     * @param message 首条问题
     * @return 会话标题
     */
    private String buildTitle(String message) {
        int maxLength = properties.getChat().getTitleMaxLength();
        return message.length() <= maxLength ? message : message.substring(0, maxLength);
    }

    /**
     * 异常转 SSE error 事件
     *
     * @param ex 异常
     * @return error 事件
     */
    private ChatEvent toErrorEvent(Throwable ex) {
        if (ex instanceof BizException bizException) {
            return ChatEvent.of(ChatEvent.EVENT_ERROR,
                    new ChatStreamPayload.Error(bizException.getCode(), bizException.getMsg()));
        }
        if (ex instanceof UnauthorizedException unauthorizedException) {
            return ChatEvent.of(ChatEvent.EVENT_ERROR,
                    new ChatStreamPayload.Error(CommonErrorConstant.UNAUTHORIZED.code(),
                            unauthorizedException.getMsg()));
        }
        // 流中途的异常基本来自模型或工具调用：对用户统一给「稍后重试」，细节留在日志里
        return ChatEvent.of(ChatEvent.EVENT_ERROR,
                new ChatStreamPayload.Error(AiAgentErrorConstant.MODEL_ERROR.code(),
                        AiAgentErrorConstant.MODEL_ERROR.msg()));
    }

    /**
     * 记录模型调用失败：把 HTTP 响应体一起打出来
     *
     * <p>Spring AI 的 OpenAI 客户端走 WebClient，异常里只有状态码，真正的原因（模型名不存在、
     * Key 无效、参数不合法、网关路径不对）在响应体里。不记下来就只能看到一句
     * {@code 400 Bad Request}，排查时还得另开一次抓包，所以这里专门把 body 带进日志。
     *
     * @param conversationId 会话 ID
     * @param ex             异常
     */
    private void logModelFailure(Long conversationId, Throwable ex) {
        if (ex instanceof WebClientResponseException responseException) {
            log.error("智能客服回答失败：conversationId={}, status={}, body={}",
                    conversationId, responseException.getStatusCode(),
                    responseException.getResponseBodyAsString(), ex);
            return;
        }
        log.error("智能客服回答失败：conversationId={}", conversationId, ex);
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

    /**
     * 计算耗时（毫秒）
     *
     * @param startedAtNanos 开始时间（纳秒）
     * @return 毫秒
     */
    private double elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000.0;
    }

    /**
     * 本轮问答上下文：一次请求里不变的信息
     *
     * @param conversationId  会话 ID
     * @param userId          用户 ID
     * @param message         本轮问题
     * @param city            城市标签，可为空
     * @param lockKey         会话互斥许可的 key
     * @param permitId        会话互斥许可的 ID：释放时必须原样传回，且可以在任意线程释放
     * @param userMessageId   本轮用户消息 ID
     */
    private record ChatContext(Long conversationId, Long userId, String message, String city,
                               String lockKey, String permitId, Long userMessageId) {

        /**
         * 会话 ID 的字符串形式：既是 Redis 记忆的 conversationId，也是 SSE 事件里回传的值
         *
         * @return 字符串形式的会话 ID
         */
        String conversationIdKey() {
            return String.valueOf(conversationId);
        }
    }
}
