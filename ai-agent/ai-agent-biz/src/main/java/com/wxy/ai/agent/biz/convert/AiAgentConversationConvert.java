package com.wxy.ai.agent.biz.convert;

import com.wxy.ai.agent.biz.po.AiAgentConversation;
import com.wxy.ai.agent.biz.vo.admin.ConversationAdminRespVO;
import com.wxy.ai.agent.biz.vo.app.ConversationRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 会话对象转换：实体到小程序端 / 管理端返回体。
 *
 * @author wxy
 * @date 2026/10/05
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiAgentConversationConvert {

    /**
     * 实体转小程序端会话行
     *
     * @param po 会话实体，可以为 null
     * @return 会话行，入参为 null 时返回 null
     */
    ConversationRespVO toRespVO(AiAgentConversation po);

    /**
     * 实体列表转小程序端会话行列表
     *
     * @param list 会话实体列表，可以为 null
     * @return 会话行列表，入参为 null 时返回 null
     */
    List<ConversationRespVO> toRespVOList(List<AiAgentConversation> list);

    /**
     * 实体转管理端会话行
     *
     * @param po 会话实体，可以为 null
     * @return 会话行，入参为 null 时返回 null
     */
    ConversationAdminRespVO toAdminRespVO(AiAgentConversation po);

    /**
     * 实体列表转管理端会话行列表
     *
     * @param list 会话实体列表，可以为 null
     * @return 会话行列表，入参为 null 时返回 null
     */
    List<ConversationAdminRespVO> toAdminRespVOList(List<AiAgentConversation> list);
}
