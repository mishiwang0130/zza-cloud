package com.wxy.ai.agent.biz.vo.app;

/**
 * SSE 事件：事件名 + 载荷。
 *
 * <p>事件顺序固定为 {@code meta → delta* → sources → done}；任何异常都转成 {@code error} 事件，
 * 保证小程序端只按事件名分发，不用同时处理「JSON 错误体」与「事件流」两套协议。
 *
 * @param name 事件名
 * @param data 事件载荷
 * @author wxy
 * @date 2026/10/05
 */
public record ChatEvent(String name, Object data) {

    /** 会话元信息，第一个事件 */
    public static final String EVENT_META = "meta";

    /** 回答增量片段（多条） */
    public static final String EVENT_DELTA = "delta";

    /** 本轮命中的知识来源 */
    public static final String EVENT_SOURCES = "sources";

    /** 本轮回答结束 */
    public static final String EVENT_DONE = "done";

    /** 异常事件 */
    public static final String EVENT_ERROR = "error";

    /**
     * 构造事件
     *
     * @param name 事件名
     * @param data 事件载荷
     * @return 事件
     */
    public static ChatEvent of(String name, Object data) {
        return new ChatEvent(name, data);
    }
}
