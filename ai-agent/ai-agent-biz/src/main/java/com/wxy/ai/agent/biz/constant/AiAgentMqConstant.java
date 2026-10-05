package com.wxy.ai.agent.biz.constant;

import com.wxy.common.mq.constant.CommonMqConstant;

/**
 * ai-agent 的 MQ topic / tag 常量：全局前缀 + 本服务模块段 + 具体业务。
 *
 * <p>topic / tag 只能用字母、数字、下划线、短横线（RocketMQ 不允许冒号），
 * 生产者与消费者引用同一份常量，避免两边字符串写岔导致消息进了没人听的 topic。
 *
 * <p>目前只有知识库索引一条链路：上传与重建索引接口只落库 + 投递消息，解析切片与向量化
 * 由消费者异步执行，接口不再让用户干等。
 *
 * @author wxy
 * @date 2026/10/05
 */
public final class AiAgentMqConstant {

    /** ai-agent 模块段：全局前缀 + 服务名，topic 都从这一段开始拼 */
    public static final String PREFIX = CommonMqConstant.PREFIX + "-ai-agent";

    /** 知识库索引 topic：上传 / 重建索引接口发，索引消费者收 */
    public static final String KNOWLEDGE_INDEX_TOPIC = PREFIX + "-knowledge-index";

    /** 知识库索引 tag：解析切片并写入向量库 */
    public static final String KNOWLEDGE_INDEX_TAG = "knowledge-index";

    /**
     * 知识库索引消费者组：同一组内的实例分摊消息。
     *
     * <p>服务多实例部署时，同一个 consumerGroup 保证一条消息只被一个实例消费一次，
     * 组名带服务前缀，避免与其他服务的消费者组重名。
     */
    public static final String KNOWLEDGE_INDEX_CONSUMER_GROUP = PREFIX + "-knowledge-index-consumer";

    /**
     * 工具类常量类，禁止实例化
     */
    private AiAgentMqConstant() {
    }
}
