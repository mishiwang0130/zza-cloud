package com.wxy.ai.agent.biz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智能客服业务配置，统一前缀 {@code zza.ai-agent}。
 *
 * <p>模型与中间件连接（DashScope、Redis 会话记忆、Qdrant）不在这里：它们分别由
 * {@code spring.ai.dashscope.*}、{@code spring.ai.memory.*}、{@code spring.ai.vectorstore.qdrant.*} 管理，
 * 本类只放「业务行为」的开关与阈值。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zza.ai-agent")
public class AiAgentProperties {

    /** 对话相关配置 */
    private final Chat chat = new Chat();

    /** 知识库检索（RAG）相关配置 */
    private final Rag rag = new Rag();

    /** 知识库文档相关配置 */
    private final Knowledge knowledge = new Knowledge();

    /** 分布式锁配置 */
    private final Lock lock = new Lock();

    /** 工具开关 */
    private final Tool tool = new Tool();

    /**
     * 对话配置
     */
    @Getter
    @Setter
    public static class Chat {

        /** 系统提示词位置（classpath 资源） */
        private String systemPromptLocation = "classpath:prompts/system-prompt.st";

        /** 单条用户消息最大字符数，超过直接按参数错误拒绝 */
        private int maxMessageLength = 2000;

        /** 会话标题最大长度：取首条用户问题截断，不额外调用模型生成标题 */
        private int titleMaxLength = 30;

        /** 小程序端一次最多拉取多少条历史消息 */
        private int maxHistorySize = 100;
    }

    /**
     * 知识库检索配置
     */
    @Getter
    @Setter
    public static class Rag {

        /** 是否启用知识库检索；关闭后退化为纯模型回答（本地零依赖调试用） */
        private boolean enabled = true;

        /** 召回条数 */
        private int topK = 5;

        /** 相似度阈值 0~1，低于该分数的片段丢弃 */
        private double similarityThreshold = 0.5;

        /** 检索异常时是否直接失败；false 表示降级为纯模型回答 */
        private boolean failFast = false;

        /** 拼进提示词的参考资料最大字符数 */
        private int maxContextChars = 4000;

        /** 城市标签在向量库 metadata 中的字段名 */
        private String cityMetadataKey = "city";

        /** 平台级通用文档的城市标签值：选了城市时「选中城市 + 通用」一起召回 */
        private String commonCity = "通用";

        /** 按城市过滤后没有命中时是否自动回退为不带过滤的检索 */
        private boolean fallbackToUnfilteredWhenEmpty = true;
    }

    /**
     * 知识库文档配置
     */
    @Getter
    @Setter
    public static class Knowledge {

        /** 上传文件大小上限（MB），要与 spring.servlet.multipart.max-file-size 保持一致 */
        private int maxFileSizeMb = 20;

        /** 切片大小（token 近似值） */
        private int chunkSize = 800;

        /** 允许断句的最小字符位置：避免切出极短碎片 */
        private int minChunkSizeChars = 350;

        /** 短于该长度的碎片不参与向量化，直接丢弃 */
        private int minChunkLengthToEmbed = 5;

        /** 单篇文档最多切成多少片，防止超大文件把向量库写爆 */
        private int maxNumChunks = 10000;

        /** 切片时是否保留原文分隔符（换行、标点） */
        private boolean keepSeparator = true;

        /** 原始文件存哪：minio=对象存储、local=本机磁盘（没有 MinIO 时本地调试） */
        private String storageType = "local";

        /** storageType=local 时的存储根目录 */
        private String localPath = "./data/knowledge";

        /** 对象存储里的目录前缀：文件存成 {prefix}{文档ID}/{文件名} */
        private String storagePrefix = "ai-agent/documents/";
    }

    /**
     * 分布式锁配置
     */
    @Getter
    @Setter
    public static class Lock {

        /** 抢锁最长等待毫秒数：0 表示不等待，拿不到立即返回「操作进行中」 */
        private long waitMillis = 0L;

        /** 单次问答的持锁租期（毫秒）：超过该时间锁自动释放，避免异常请求把会话永久锁死 */
        private long chatLeaseMillis = 120_000L;

        /** 单篇文档重建/删除的持锁租期（毫秒）：解析与向量化比问答慢 */
        private long knowledgeLeaseMillis = 300_000L;
    }

    /**
     * 工具开关
     */
    @Getter
    @Setter
    public static class Tool {

        /** 是否向模型注册工具；关闭后模型只依据知识库与提示词回答 */
        private boolean enabled = true;
    }
}
