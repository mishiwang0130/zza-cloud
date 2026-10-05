package com.wxy.ai.agent.biz.config;

import com.wxy.ai.agent.biz.tool.LeaseTools;
import com.wxy.ai.agent.biz.tool.KnowledgeTools;
import com.wxy.ai.agent.biz.tool.RentCalculatorTools;
import com.wxy.ai.agent.biz.tool.RoomTools;
import com.wxy.ai.agent.biz.tool.ViewingTools;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * ChatClient 装配：系统提示词 + 多轮会话记忆 + 工具。
 *
 * <p>会话记忆用 Spring AI Alibaba 的 Redis 实现（{@code spring-ai-alibaba-starter-memory-redis}），
 * 这里只负责把它挂成默认顾问：多轮上下文由框架按 conversationId 读写，不手写编解码与 Repository。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Slf4j
@Configuration
public class AiAgentChatClientConfiguration {

    /** 系统提示词缺失时的兜底文案：宁可少说话，也不要让服务因为少一个文件起不来 */
    private static final String FALLBACK_SYSTEM_PROMPT = """
            你是租房平台的智能客服，负责回答房源、看房预约、租约与费用等租赁相关问题。
            请使用简体中文、语气友好简洁；价格与房源信息必须来自工具查询结果或参考资料，不要编造。
            """;

    /**
     * 构造 ChatClient
     *
     * @param builder        Spring AI 提供的构建器（已带 DashScope 的 ChatModel）
     * @param chatMemory      会话记忆（Redis 实现）
     * @param properties       业务配置
     * @param resourceLoader   资源加载器（读系统提示词）
     * @param knowledgeTools   知识库工具（本地实现）
     * @param rentCalculatorTools 费用试算工具（本地实现）
     * @param roomTools        房源工具（远程调用待接入）
     * @param viewingTools     看房预约工具（远程调用待接入）
     * @param leaseTools       租约工具（远程调用待接入）
     * @return ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory,
                                 AiAgentProperties properties,
                                 ResourceLoader resourceLoader,
                                 ObjectProvider<KnowledgeTools> knowledgeTools,
                                 ObjectProvider<RentCalculatorTools> rentCalculatorTools,
                                 ObjectProvider<RoomTools> roomTools,
                                 ObjectProvider<ViewingTools> viewingTools,
                                 ObjectProvider<LeaseTools> leaseTools) {
        ChatClient.Builder clientBuilder = builder
                .defaultSystem(loadSystemPrompt(resourceLoader, properties))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 原样输出提示词：知识库原文里的花括号不该被当成模板变量
                .defaultTemplateRenderer(new PlainTextTemplateRenderer());
        List<Object> tools = collectTools(properties, knowledgeTools, rentCalculatorTools,
                roomTools, viewingTools, leaseTools);
        if (!tools.isEmpty()) {
            clientBuilder = clientBuilder.defaultTools(tools.toArray());
        }
        log.info("智能客服 ChatClient 初始化完成，注册工具 {} 个", tools.size());
        return clientBuilder.build();
    }

    /**
     * 收集要注册的工具：{@code zza.ai-agent.tool.enabled=false} 时一个都不注册
     *
     * @param properties          业务配置
     * @param knowledgeTools      知识库工具
     * @param rentCalculatorTools 费用试算工具
     * @param roomTools           房源工具
     * @param viewingTools        看房预约工具
     * @param leaseTools          租约工具
     * @return 工具列表
     */
    private List<Object> collectTools(AiAgentProperties properties,
                                      ObjectProvider<KnowledgeTools> knowledgeTools,
                                      ObjectProvider<RentCalculatorTools> rentCalculatorTools,
                                      ObjectProvider<RoomTools> roomTools,
                                      ObjectProvider<ViewingTools> viewingTools,
                                      ObjectProvider<LeaseTools> leaseTools) {
        List<Object> tools = new ArrayList<>();
        if (!properties.getTool().isEnabled()) {
            return tools;
        }
        knowledgeTools.ifAvailable(tools::add);
        rentCalculatorTools.ifAvailable(tools::add);
        roomTools.ifAvailable(tools::add);
        viewingTools.ifAvailable(tools::add);
        leaseTools.ifAvailable(tools::add);
        return tools;
    }

    /**
     * 读取系统提示词
     *
     * @param resourceLoader 资源加载器
     * @param properties     业务配置
     * @return 提示词文本，文件不存在时用兜底文案
     */
    private String loadSystemPrompt(ResourceLoader resourceLoader, AiAgentProperties properties) {
        String location = properties.getChat().getSystemPromptLocation();
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            log.warn("未找到系统提示词 {}，使用内置兜底提示词", location);
            return FALLBACK_SYSTEM_PROMPT;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("系统提示词读取失败：" + location, ex);
        }
    }
}
