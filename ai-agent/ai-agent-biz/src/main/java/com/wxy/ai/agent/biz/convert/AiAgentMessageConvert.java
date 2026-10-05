package com.wxy.ai.agent.biz.convert;

import com.wxy.ai.agent.biz.po.AiAgentMessage;
import com.wxy.ai.agent.biz.vo.admin.ConversationMessageRespVO;
import com.wxy.ai.agent.biz.vo.app.MessageRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 会话消息对象转换：实体到小程序端 / 管理端返回体。
 *
 * <p>{@code sources} 在库里是 JSON 字符串，需要 Fastjson2 解析成结构化来源，
 * 不能靠 MapStruct 直接拷贝，所以这里 {@code ignore}，由 Service 解析后回填。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiAgentMessageConvert {

    /**
     * 实体转小程序端消息
     *
     * @param po 消息实体，可以为 null
     * @return 消息返回体，入参为 null 时返回 null
     */
    @Mapping(target = "sources", ignore = true)
    MessageRespVO toRespVO(AiAgentMessage po);

    /**
     * 实体列表转小程序端消息列表
     *
     * @param list 消息实体列表，可以为 null
     * @return 消息列表，入参为 null 时返回 null
     */
    List<MessageRespVO> toRespVOList(List<AiAgentMessage> list);

    /**
     * 实体转管理端消息
     *
     * @param po 消息实体，可以为 null
     * @return 消息返回体，入参为 null 时返回 null
     */
    @Mapping(target = "sources", ignore = true)
    ConversationMessageRespVO toAdminRespVO(AiAgentMessage po);

    /**
     * 实体列表转管理端消息列表
     *
     * @param list 消息实体列表，可以为 null
     * @return 消息列表，入参为 null 时返回 null
     */
    List<ConversationMessageRespVO> toAdminRespVOList(List<AiAgentMessage> list);
}
