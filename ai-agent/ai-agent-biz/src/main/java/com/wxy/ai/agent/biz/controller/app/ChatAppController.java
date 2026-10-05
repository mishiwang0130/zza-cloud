package com.wxy.ai.agent.biz.controller.app;

import com.wxy.ai.agent.biz.service.ChatService;
import com.wxy.ai.agent.biz.vo.app.ChatRespVO;
import com.wxy.ai.agent.biz.vo.app.ChatStreamReqVO;
import com.wxy.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 小程序端智能客服对话接口：最终对外路径为 {@code /api/ai-agent/app-api/chat/...}。
 *
 * <p>两个接口都要求登录（没有 {@code @PermitAll}），由公共凭证拦截器按 {@code /app-api} 前缀
 * 校验用户端身份；SSE 无法自定义请求头，小程序端用 {@code ?token=} 传令牌
 * （{@code zza.security.token-parameter} 已配置为 {@code token}）。
 *
 * <p>流式接口刻意不加 {@code @Validated}：参数不合法时也要返回 {@code error} 事件，
 * 保证这个地址只会输出事件流，前端不需要再兼容一种 JSON 错误体。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Tag(name = "用户端 - 智能客服")
@RestController
@RequestMapping("/chat")
public class ChatAppController {

    /** 对话服务 */
    @Resource
    private ChatService chatService;

    /**
     * 流式提问
     *
     * @param reqVO 提问入参
     * @return SSE 事件流
     */
    @Operation(summary = "流式提问（SSE）",
            description = "事件顺序：meta → delta（多条）→ sources → done；异常时返回 error 事件。"
                    + "小程序端用 wx.request 的 enableChunked 解析，令牌用 ?token= 传递")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(@RequestBody ChatStreamReqVO reqVO) {
        // Service 返回业务事件，这里只做一层适配：事件名映射成 SSE 的 event 字段，载荷映射成 data
        return chatService.stream(reqVO)
                .map(event -> ServerSentEvent.builder(event.data()).event(event.name()).build());
    }

    /**
     * 非流式提问
     *
     * @param reqVO 提问入参
     * @return 完整回答
     */
    @Operation(summary = "非流式提问", description = "用于接口文档调试与脚本化回归")
    @PostMapping("/ask")
    public Result<ChatRespVO> ask(@Validated @RequestBody ChatStreamReqVO reqVO) {
        return Result.success(chatService.ask(reqVO));
    }
}
