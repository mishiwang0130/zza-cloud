package com.wxy.ai.agent.biz.vo.app;

import java.util.List;

/**
 * SSE 各类事件的载荷定义，与小程序端的事件解析一一对应。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class ChatStreamPayload {

    /**
     * 会话元信息
     *
     * @param conversationId 会话 ID
     * @param userMessageId  本轮用户消息 ID
     */
    public record Meta(String conversationId, Long userMessageId) {
    }

    /**
     * 回答增量
     *
     * @param content 增量文本
     */
    public record Delta(String content) {
    }

    /**
     * 本轮命中的知识来源
     *
     * @param sources 来源列表
     */
    public record Sources(List<ChatSourceRespVO> sources) {
    }

    /**
     * 本轮结束
     *
     * @param conversationId    会话 ID
     * @param assistantMessageId 本轮 AI 消息 ID
     * @param elapsedMillis      回答耗时（毫秒）
     */
    public record Done(String conversationId, Long assistantMessageId, double elapsedMillis) {
    }

    /**
     * 异常
     *
     * @param code    错误码
     * @param message 面向用户的提示
     */
    public record Error(int code, String message) {
    }

    /**
     * 工具类，禁止实例化
     */
    private ChatStreamPayload() {
    }
}
