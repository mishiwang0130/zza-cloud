package com.wxy.ai.agent.biz.constant;

/**
 * 智能客服管理端权限标识：与 {@code infra_menu.perms} 一一对应。
 *
 * <p>用户端（小程序）接口不挂权限：登录即可用，且只能操作自己的会话。
 * 管理端的知识库与会话记录接口必须挂这些标识，由 common-webmvc 的权限拦截器校验。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class AiAgentPermissionConstant {

    /** 知识库查询：文档列表、语义检索调试 */
    public static final String KNOWLEDGE_QUERY = "ai-agent:knowledge:query";

    /** 知识库上传 */
    public static final String KNOWLEDGE_CREATE = "ai-agent:knowledge:create";

    /** 知识库重建索引 */
    public static final String KNOWLEDGE_REBUILD = "ai-agent:knowledge:rebuild";

    /** 知识库删除 */
    public static final String KNOWLEDGE_DELETE = "ai-agent:knowledge:delete";

    /** 会话记录查询：后台查看会话与消息明细 */
    public static final String CONVERSATION_QUERY = "ai-agent:conversation:query";

    /**
     * 工具类常量类，禁止实例化
     */
    private AiAgentPermissionConstant() {
    }
}
