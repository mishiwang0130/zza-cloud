package com.wxy.ai.agent.biz.service;

import com.wxy.ai.agent.biz.vo.app.ChatEvent;
import com.wxy.ai.agent.biz.vo.app.ChatRespVO;
import com.wxy.ai.agent.biz.vo.app.ChatStreamReqVO;
import reactor.core.publisher.Flux;

/**
 * 智能客服对话能力：流式（SSE）与非流式两条入口共用同一套准备与落库逻辑。
 *
 * @author wxy
 * @date 2026/10/05
 */
public interface ChatService {

    /**
     * 流式提问：事件顺序 {@code meta → delta* → sources → done}，异常给 {@code error}
     *
     * <p>参数错误、会话归属错误、并发冲突这些「流开始前」的失败也会包装成 {@code error} 事件，
     * 保证接口只输出一种协议（事件流），小程序端不需要同时处理 JSON 错误体。
     *
     * @param reqVO 提问入参
     * @return 事件流
     */
    Flux<ChatEvent> stream(ChatStreamReqVO reqVO);

    /**
     * 非流式提问：用于接口文档调试与脚本化回归
     *
     * @param reqVO 提问入参
     * @return 完整回答
     */
    ChatRespVO ask(ChatStreamReqVO reqVO);
}
