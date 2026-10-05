package com.wxy.ai.agent.biz.constant;

import com.wxy.common.core.result.ErrorCode;

/**
 * ai-agent 业务错误码常量：服务位固定 {@code 04}，模块位 000 通用、001 会话、002 知识库。
 *
 * <p>公共错误码（参数错误、未登录、无权限、系统异常等）用 {@code CommonErrorConstant}，
 * 这里只放智能客服自己的业务错误；业务代码只引用常量，禁止出现数字字面量。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class AiAgentErrorConstant {

    // ==================== 000 跨模块通用 ====================

    /** 大模型调用失败：模型服务不可用、超时、返回体解析失败等 */
    public static final ErrorCode MODEL_ERROR = new ErrorCode(1_04_000_0001, "智能客服暂时不可用，请稍后重试");

    // ==================== 001 会话与消息 ====================

    /** 会话不存在：按 ID 查不到未删除的会话，或该会话不属于当前登录用户 */
    public static final ErrorCode CONVERSATION_NOT_FOUND = new ErrorCode(1_04_001_0001, "会话不存在");

    /** 会话正在回答上一条消息：同一会话不允许并发提问（分布式锁未抢到） */
    public static final ErrorCode CONVERSATION_BUSY = new ErrorCode(1_04_001_0002, "正在回答上一条消息，请稍候");

    // ==================== 002 知识库 ====================

    /** 知识库文档不存在 */
    public static final ErrorCode KNOWLEDGE_DOCUMENT_NOT_FOUND = new ErrorCode(1_04_002_0001, "知识库文档不存在");

    /** 文件类型不支持：只支持 md / txt / pdf / docx 等可解析文本的文档 */
    public static final ErrorCode KNOWLEDGE_FILE_TYPE_UNSUPPORTED = new ErrorCode(1_04_002_0002, "不支持该文件类型");

    /** 文档解析失败：文件损坏、加密 PDF、内容为空等 */
    public static final ErrorCode KNOWLEDGE_DOCUMENT_PARSE_FAILED = new ErrorCode(1_04_002_0003, "文档解析失败");

    /** 上传文件超过大小上限 */
    public static final ErrorCode KNOWLEDGE_FILE_TOO_LARGE = new ErrorCode(1_04_002_0004, "文件超过大小上限");

    /** 向量化失败：切片写入向量库时出错（模型或向量库不可用） */
    public static final ErrorCode KNOWLEDGE_INDEX_FAILED = new ErrorCode(1_04_002_0005, "知识库索引失败");

    /** 文档正在被重建或删除：同一文档的写操作必须串行（分布式锁未抢到） */
    public static final ErrorCode KNOWLEDGE_DOCUMENT_BUSY = new ErrorCode(1_04_002_0006, "该文档正在处理中，请稍候");

    /** 索引任务投递失败：MQ 未配置或 NameServer / Broker 不可达，文档停在「待索引」没有意义 */
    public static final ErrorCode KNOWLEDGE_INDEX_MESSAGE_FAILED =
            new ErrorCode(1_04_002_0007, "索引任务投递失败，请稍后重试");

    /**
     * 工具类常量类，禁止实例化
     */
    private AiAgentErrorConstant() {
    }
}
