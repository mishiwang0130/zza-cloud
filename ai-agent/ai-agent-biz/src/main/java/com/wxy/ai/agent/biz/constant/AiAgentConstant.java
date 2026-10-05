package com.wxy.ai.agent.biz.constant;

/**
 * ai-agent 业务常量：不随环境变化的固定值。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class AiAgentConstant {

    /** 工具上下文里的登录用户 ID 键：ChatService 从令牌身份里取出，随 toolContext 传给工具 */
    public static final String TOOL_CONTEXT_USER_ID = "userId";

    /** 参考资料在 AI 消息里的展示名前缀：来源知识库文档 */
    public static final String SOURCE_TYPE_KNOWLEDGE = "knowledge";

    /**
     * 工具类常量类，禁止实例化
     */
    private AiAgentConstant() {
    }
}
